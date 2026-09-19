import pytest
import asyncio
from unittest.mock import MagicMock, patch
from fastapi.testclient import TestClient
from app.main import app
from app.config import settings
from app.models import ChatRequest
from app.gemini_service import _call_gemini_api, generate_ai_response, _generate_local_pedagogical_fallback

client = TestClient(app)

def test_health_endpoint_returns_gemini_model_and_zero_secrets():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert "environment" in data
    assert "geminiConfigured" in data
    assert "groqConfigured" in data
    assert "ollamaConfigured" in data
    assert isinstance(data["geminiConfigured"], bool)
    assert isinstance(data["groqConfigured"], bool)
    assert isinstance(data["ollamaConfigured"], bool)
    assert data["model"] == "gemini-2.5-flash-lite"
    assert "gemini_api_key" not in data
    assert "groq_api_key" not in data
    assert "apiKey" not in data
    assert "secret" not in str(data).lower()

def test_missing_token_returns_401():
    payload = {
        "message": "Hello AI",
        "conversationId": "test_conv_1",
        "targetLanguage": "English",
        "motherTongue": "Tamil",
        "level": "Beginner",
        "mode": "AI_FRIEND"
    }
    response = client.post("/v1/ai/chat", json=payload)
    assert response.status_code == 401
    assert "Missing Authorization" in response.json()["detail"]

def test_invalid_token_returns_401():
    payload = {
        "message": "Hello AI",
        "conversationId": "test_conv_1",
        "targetLanguage": "English",
        "motherTongue": "Tamil",
        "level": "Beginner",
        "mode": "AI_FRIEND"
    }
    headers = {"Authorization": "Bearer invalid_token"}
    response = client.post("/v1/ai/chat", json=payload, headers=headers)
    assert response.status_code == 401

def test_malformed_request_returns_422():
    # Missing required 'message' field
    payload = {
        "conversationId": "test_conv_1",
        "targetLanguage": "English"
    }
    headers = {"Authorization": "Bearer test_user_token_12345"}
    response = client.post("/v1/ai/chat", json=payload, headers=headers)
    assert response.status_code == 422

def test_rate_limiter_returns_429():
    from app.main import user_request_timestamps

    headers = {"Authorization": "Bearer test_user_rate_limit_uid"}
    payload = {
        "message": "Test rate limiting",
        "targetLanguage": "English",
        "motherTongue": "Tamil",
        "level": "Beginner",
        "mode": "AI_FRIEND"
    }

    # Temporarily set limit to 2
    original_limit = settings.rate_limit_per_minute
    settings.rate_limit_per_minute = 2
    try:
        user_request_timestamps["user_rate_limit_uid"].clear()
        res1 = client.post("/v1/ai/chat", json=payload, headers=headers)
        assert res1.status_code == 200
        res2 = client.post("/v1/ai/chat", json=payload, headers=headers)
        assert res2.status_code == 200
        res3 = client.post("/v1/ai/chat", json=payload, headers=headers)
        assert res3.status_code == 429
        assert "Rate limit exceeded" in res3.json()["detail"]
    finally:
        settings.rate_limit_per_minute = original_limit

@pytest.mark.anyio
async def test_ai_friend_gemini_success_sets_is_fallback_false():
    mock_client = MagicMock()
    mock_model_response = MagicMock()
    mock_model_response.text = '{"reply": "Hey bro! How is your day going? Let us chat about tech!", "corrections": [], "suggestions": ["Tell me a story", "Practice English"]}'
    mock_client.models.generate_content.return_value = mock_model_response

    request = ChatRequest(
        message="Hello bro",
        conversationId="test_ai_friend",
        targetLanguage="English",
        motherTongue="Tamil",
        level="Beginner",
        mode="AI_FRIEND"
    )

    response = await _call_gemini_api(request, client=mock_client)
    assert response.success is True
    assert response.isFallback is False
    assert "Hey bro" in response.reply
    assert response.action is None
    assert len(response.suggestions) == 2

@pytest.mark.anyio
async def test_ai_assistant_returns_structured_action():
    mock_client = MagicMock()
    mock_model_response = MagicMock()
    mock_model_response.text = '{"reply": "Opening YouTube for you now, bro!", "action": {"intent": "OPEN_APP", "appName": "YouTube", "parameters": {}, "requiresConfirmation": false}, "corrections": [], "suggestions": ["Search songs", "Go back"]}'
    mock_client.models.generate_content.return_value = mock_model_response

    request = ChatRequest(
        message="Open YouTube",
        conversationId="test_assistant_action",
        targetLanguage="English",
        motherTongue="Tamil",
        level="Beginner",
        mode="AI_ASSISTANT"
    )

    response = await _call_gemini_api(request, client=mock_client)
    assert response.success is True
    assert response.isFallback is False
    assert response.action is not None
    assert response.action.intent == "OPEN_APP"
    assert response.action.appName == "YouTube"
    assert response.action.requiresConfirmation is False

@pytest.mark.anyio
async def test_ai_assistant_normal_conversation_returns_no_action():
    mock_client = MagicMock()
    mock_model_response = MagicMock()
    mock_model_response.text = '{"reply": "I am doing great bro, thanks for asking! How can I help you on your phone today?", "action": null, "corrections": [], "suggestions": ["Open Settings", "Set an alarm"]}'
    mock_client.models.generate_content.return_value = mock_model_response

    request = ChatRequest(
        message="How are you?",
        conversationId="test_assistant_chat",
        targetLanguage="English",
        motherTongue="Tamil",
        level="Beginner",
        mode="AI_ASSISTANT"
    )

    response = await _call_gemini_api(request, client=mock_client)
    assert response.success is True
    assert response.isFallback is False
    assert response.action is None
    assert "great bro" in response.reply.lower()

@pytest.mark.anyio
async def test_grammar_correction_with_gemini():
    mock_client = MagicMock()
    mock_model_response = MagicMock()
    mock_model_response.text = '{"reply": "Here is the correction: Yesterday I went to college.", "action": null, "corrections": [{"original": "am go to", "corrected": "went to", "explanation": "Use past tense went for past actions.", "rule": "Simple Past Tense"}], "suggestions": ["Explain past tense"]}'
    mock_client.models.generate_content.return_value = mock_model_response

    request = ChatRequest(
        message="I am go to college yesterday.",
        conversationId="test_grammar_mode",
        targetLanguage="English",
        motherTongue="Tamil",
        level="Beginner",
        mode="GRAMMAR_CORRECTION"
    )

    response = await _call_gemini_api(request, client=mock_client)
    assert response.success is True
    assert response.isFallback is False
    assert len(response.corrections) == 1
    assert response.corrections[0].corrected == "went to"

@pytest.mark.anyio
async def test_tanglish_persona_prompting():
    mock_client = MagicMock()
    mock_model_response = MagicMock()
    mock_model_response.text = '{"reply": "Naan nalla irukken bro! Neenga epdi irukinga?", "action": null, "corrections": [], "suggestions": ["Practice English", "Tamil news"]}'
    mock_client.models.generate_content.return_value = mock_model_response

    request = ChatRequest(
        message="Bro epdi iruka?",
        conversationId="test_tanglish",
        targetLanguage="English",
        motherTongue="Tamil",
        level="Beginner",
        mode="AI_FRIEND"
    )

    response = await _call_gemini_api(request, client=mock_client)
    assert response.success is True
    assert response.isFallback is False
    assert "nalla irukken bro" in response.reply

@pytest.mark.anyio
async def test_tamil_script_input():
    mock_client = MagicMock()
    mock_model_response = MagicMock()
    mock_model_response.text = '{"reply": "வணக்கம் நண்பா! நான் நன்றாக இருக்கிறேன்.", "action": null, "corrections": [], "suggestions": ["ஆங்கிலம் கற்கலாம்"]}'
    mock_client.models.generate_content.return_value = mock_model_response

    request = ChatRequest(
        message="நீ எப்படி இருக்க?",
        conversationId="test_tamil_script",
        targetLanguage="English",
        motherTongue="Tamil",
        level="Beginner",
        mode="AI_FRIEND"
    )

    response = await _call_gemini_api(request, client=mock_client)
    assert response.success is True
    assert response.isFallback is False
    assert "வணக்கம்" in response.reply

@pytest.mark.anyio
async def test_api_key_missing_returns_clear_error():
    original_key = settings.gemini_api_key
    try:
        settings.gemini_api_key = ""
        request = ChatRequest(
            message="Hello",
            conversationId="test_no_key",
            targetLanguage="English",
            motherTongue="Tamil",
            level="Beginner",
            mode="AI_FRIEND"
        )
        response = await generate_ai_response(request)
        assert response.success is False
        assert response.isFallback is False
        assert response.errorCode == "API_KEY_MISSING"
        assert "GEMINI_API_KEY in backend/.env" in response.reply
    finally:
        settings.gemini_api_key = original_key

@pytest.mark.anyio
async def test_gemini_rate_limited_429_handling():
    original_key = settings.gemini_api_key
    settings.gemini_api_key = "test_key_rate_limited"

    try:
        with patch("app.gemini_service.get_gemini_client") as mock_get_client:
            mock_client = MagicMock()
            mock_client.models.generate_content.side_effect = Exception("429 ResourceExhausted: Quota exceeded")
            mock_get_client.return_value = mock_client

            request = ChatRequest(
                message="Hello",
                conversationId="test_429",
                targetLanguage="English",
                motherTongue="Tamil",
                level="Beginner",
                mode="AI_FRIEND"
            )

            response = await generate_ai_response(request)
            assert response.success is False
            assert response.isFallback is False
            assert response.errorCode == "RATE_LIMITED"
            assert "temporarily unavailable" in response.reply
    finally:
        settings.gemini_api_key = original_key

@pytest.mark.anyio
async def test_gemini_timeout_handling():
    original_key = settings.gemini_api_key
    settings.gemini_api_key = "test_key_timeout"

    try:
        with patch("app.gemini_service._call_gemini_api", side_effect=TimeoutError("Gemini timed out")):
            request = ChatRequest(
                message="Hello",
                conversationId="test_timeout",
                targetLanguage="English",
                motherTongue="Tamil",
                level="Beginner",
                mode="AI_FRIEND"
            )

            response = await generate_ai_response(request)
            assert response.success is False
            assert response.isFallback is False
            assert response.errorCode == "TIMEOUT"
            assert "timed out" in response.reply.lower()
    finally:
        settings.gemini_api_key = original_key

def test_api_key_never_exposed_in_logs_or_response():
    test_secret = "AIzaSySecretNeverExposeInLogsOrJson99999"
    original_key = settings.gemini_api_key
    try:
        settings.gemini_api_key = test_secret
        res = client.get("/health")
        assert res.status_code == 200
        assert test_secret not in res.text

        # Test error response does not leak secret
        payload = {
            "message": "Hello",
            "targetLanguage": "English",
            "motherTongue": "Tamil",
            "level": "Beginner",
            "mode": "AI_FRIEND"
        }
        res_error = client.post("/v1/ai/chat", json=payload, headers={"Authorization": "Bearer test_user_sec_audit"})
        assert test_secret not in res_error.text
    finally:
        settings.gemini_api_key = original_key


@pytest.mark.anyio
async def test_ai_router_gemini_priority_success():
    from app.ai_router import route_ai_request
    from app.models import ChatResponse

    request = ChatRequest(
        message="Hello bro",
        conversationId="test_router_gemini",
        targetLanguage="English",
        motherTongue="Tamil",
        level="Beginner",
        mode="AI_FRIEND"
    )

    with patch("app.ai_router.generate_gemini_response") as mock_gemini:
        mock_gemini.return_value = ChatResponse(
            success=True,
            reply="Hey bro from Gemini!",
            provider="gemini",
            fallbackUsed=False,
            isFallback=False,
            conversationId=request.conversationId,
            mode=request.mode
        )
        with patch.object(settings, "gemini_api_key", "test_gemini_key"):
            response = await route_ai_request(request)
            assert response.success is True
            assert response.provider == "gemini"
            assert response.fallbackUsed is False
            assert "Gemini" in response.reply


@pytest.mark.anyio
async def test_ai_router_cascades_to_groq_when_gemini_fails():
    from app.ai_router import route_ai_request
    from app.models import ChatResponse

    request = ChatRequest(
        message="Hello bro",
        conversationId="test_router_groq",
        targetLanguage="English",
        motherTongue="Tamil",
        level="Beginner",
        mode="AI_FRIEND"
    )

    with patch("app.ai_router.generate_gemini_response") as mock_gemini:
        # Gemini fails with RATE_LIMITED
        mock_gemini.return_value = ChatResponse(
            success=False,
            reply="Quota reached",
            errorCode="RATE_LIMITED",
            provider="gemini",
            fallbackUsed=False
        )
        with patch("app.ai_router.generate_groq_response") as mock_groq:
            mock_groq.return_value = ChatResponse(
                success=True,
                reply="Hey bro from Groq fallback!",
                provider="groq",
                fallbackUsed=True,
                isFallback=False,
                conversationId=request.conversationId,
                mode=request.mode
            )
            with patch.object(settings, "gemini_api_key", "test_gemini_key"):
                with patch.object(settings, "groq_api_key", "test_groq_key"):
                    response = await route_ai_request(request)
                    assert response.success is True
                    assert response.provider == "groq"
                    assert response.fallbackUsed is True
                    assert "Groq" in response.reply


@pytest.mark.anyio
async def test_ai_router_cascades_to_ollama_when_gemini_and_groq_fail():
    from app.ai_router import route_ai_request
    from app.models import ChatResponse

    request = ChatRequest(
        message="Hello bro",
        conversationId="test_router_ollama",
        targetLanguage="English",
        motherTongue="Tamil",
        level="Beginner",
        mode="AI_FRIEND"
    )

    with patch("app.ai_router.generate_gemini_response") as mock_gemini:
        mock_gemini.return_value = ChatResponse(
            success=False,
            reply="Failed",
            errorCode="AI_SERVER_ERROR",
            provider="gemini"
        )
        with patch("app.ai_router.generate_groq_response") as mock_groq:
            mock_groq.return_value = ChatResponse(
                success=False,
                reply="Groq failed",
                errorCode="TIMEOUT",
                provider="groq"
            )
            with patch("app.ai_router.generate_ollama_response") as mock_ollama:
                mock_ollama.return_value = ChatResponse(
                    success=True,
                    reply="Hey bro from local Ollama!",
                    provider="ollama",
                    fallbackUsed=True,
                    isFallback=False,
                    conversationId=request.conversationId,
                    mode=request.mode
                )
                with patch("app.ollama_service.is_ollama_available", return_value=True):
                    with patch.object(settings, "gemini_api_key", "test_gemini_key"):
                        with patch.object(settings, "groq_api_key", "test_groq_key"):
                            response = await route_ai_request(request)
                            assert response.success is True
                            assert response.provider == "ollama"
                            assert response.fallbackUsed is True
                            assert "Ollama" in response.reply


@pytest.mark.anyio
async def test_ai_router_cascades_to_offline_pedagogical_engine():
    from app.ai_router import route_ai_request

    request = ChatRequest(
        message="I am go to college yesterday.",
        conversationId="test_router_offline",
        targetLanguage="English",
        motherTongue="Tamil",
        level="Beginner",
        mode="GRAMMAR_CORRECTION"
    )

    # All keys empty / providers unavailable
    with patch.object(settings, "gemini_api_key", ""):
        with patch.object(settings, "groq_api_key", ""):
            response = await route_ai_request(request)
            assert response.success is True
            assert response.provider == "offline"
            assert response.fallbackUsed is True
            assert response.isFallback is True
            assert len(response.corrections) >= 1
            assert "went to" in response.corrections[0].corrected


@pytest.mark.anyio
async def test_groq_not_configured_error():
    from app.groq_service import generate_groq_response

    request = ChatRequest(
        message="Hello",
        conversationId="test_groq_unconfigured",
        targetLanguage="English",
        motherTongue="Tamil",
        level="Beginner",
        mode="AI_FRIEND"
    )
    with patch.object(settings, "groq_api_key", ""):
        res = await generate_groq_response(request)
        assert res.success is False
        assert res.errorCode == "NOT_CONFIGURED"


@pytest.mark.anyio
async def test_ollama_not_available_error():
    from app.ollama_service import generate_ollama_response

    request = ChatRequest(
        message="Hello",
        conversationId="test_ollama_unavail",
        targetLanguage="English",
        motherTongue="Tamil",
        level="Beginner",
        mode="AI_FRIEND"
    )
    # Target port 9999 where nothing runs to trigger fast ConnectError
    with patch.object(settings, "ollama_base_url", "http://127.0.0.1:9999"):
        res = await generate_ollama_response(request)
        assert res.success is False
        assert res.errorCode == "NOT_AVAILABLE"

