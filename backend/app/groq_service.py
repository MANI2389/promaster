import json
import logging
import httpx
from typing import Optional, List
from app.config import settings
from app.models import ChatRequest, ChatResponse, CorrectionItem, StructuredAction
from app.gemini_service import build_system_instruction

logger = logging.getLogger("promaster.groq")

GROQ_CHAT_COMPLETIONS_URL = "https://api.groq.com/openai/v1/chat/completions"


async def generate_groq_response(request: ChatRequest) -> ChatResponse:
    """
    Executes an AI request against the Groq Cloud API using the configured Groq model.
    Enforces bounded timeouts, structured JSON responses, and mapped error codes.
    """
    if not settings.groq_api_key or not settings.groq_api_key.strip():
        logger.info("Groq API key is not configured.")
        return ChatResponse(
            success=False,
            reply="Groq provider is not configured.",
            errorCode="NOT_CONFIGURED",
            conversationId=request.conversationId,
            mode=request.mode,
            isFallback=False,
            provider="groq",
            fallbackUsed=True
        )

    system_instruction = build_system_instruction(
        mode=request.mode,
        target_lang=request.targetLanguage,
        mother_tongue=request.motherTongue,
        level=request.level,
        locale=request.locale
    )

    messages = [{"role": "system", "content": system_instruction}]

    # Append brief recent history if available
    if request.history:
        for turn in request.history[-4:]:
            role = turn.get("role", "user")
            content = turn.get("content", "")
            if content and role in ("user", "assistant"):
                messages.append({"role": role, "content": content[:500]})

    messages.append({"role": "user", "content": request.message})

    payload = {
        "model": settings.groq_model,
        "messages": messages,
        "temperature": 0.7,
        "max_tokens": settings.max_output_tokens,
        "response_format": {"type": "json_object"}
    }

    headers = {
        "Authorization": f"Bearer {settings.groq_api_key}",
        "Content-Type": "application/json"
    }

    try:
        async with httpx.AsyncClient(timeout=settings.groq_timeout_seconds) as client:
            response = await client.post(
                GROQ_CHAT_COMPLETIONS_URL,
                headers=headers,
                json=payload
            )

        if response.status_code == 200:
            return _parse_groq_response(request, response.json())
        elif response.status_code == 429:
            logger.warning("Groq API rate limited (429)")
            return ChatResponse(
                success=False,
                reply="AI rate limit reached on Groq.",
                errorCode="RATE_LIMITED",
                conversationId=request.conversationId,
                mode=request.mode,
                isFallback=False,
                provider="groq",
                fallbackUsed=True
            )
        elif response.status_code in (401, 403):
            logger.error("Groq authentication failed (%d)", response.status_code)
            return ChatResponse(
                success=False,
                reply="Groq authentication failed.",
                errorCode="AUTH_ERROR",
                conversationId=request.conversationId,
                mode=request.mode,
                isFallback=False,
                provider="groq",
                fallbackUsed=True
            )
        else:
            logger.warning("Groq API returned HTTP %d: %s", response.status_code, response.text[:200])
            return ChatResponse(
                success=False,
                reply="Groq provider error.",
                errorCode="AI_SERVER_ERROR",
                conversationId=request.conversationId,
                mode=request.mode,
                isFallback=False,
                provider="groq",
                fallbackUsed=True
            )

    except httpx.TimeoutException as te:
        logger.warning("Groq API request timed out: %s", te)
        return ChatResponse(
            success=False,
            reply="Groq request timed out.",
            errorCode="TIMEOUT",
            conversationId=request.conversationId,
            mode=request.mode,
            isFallback=False,
            provider="groq",
            fallbackUsed=True
        )
    except (httpx.ConnectError, httpx.NetworkError) as ne:
        logger.warning("Groq network connection error: %s", ne)
        return ChatResponse(
            success=False,
            reply="Groq network connection failed.",
            errorCode="NETWORK_ERROR",
            conversationId=request.conversationId,
            mode=request.mode,
            isFallback=False,
            provider="groq",
            fallbackUsed=True
        )
    except Exception as e:
        logger.error("Unexpected error in Groq provider: %s", e)
        return ChatResponse(
            success=False,
            reply="Groq provider failed.",
            errorCode="AI_SERVER_ERROR",
            conversationId=request.conversationId,
            mode=request.mode,
            isFallback=False,
            provider="groq",
            fallbackUsed=True
        )


def _parse_groq_response(request: ChatRequest, response_data: dict) -> ChatResponse:
    """
    Parses OpenAI-compatible response format from Groq.
    """
    choices = response_data.get("choices", [])
    if not choices:
        raise ValueError("Groq returned empty choices")

    raw_text = choices[0].get("message", {}).get("content", "").strip()

    # Clean markdown fences if present
    cleaned = raw_text
    if cleaned.startswith("```json"):
        cleaned = cleaned[7:]
    elif cleaned.startswith("```"):
        cleaned = cleaned[3:]
    if cleaned.endswith("```"):
        cleaned = cleaned[:-3]
    cleaned = cleaned.strip()

    try:
        data = json.loads(cleaned)
    except Exception:
        # If response wasn't valid JSON, wrap raw text as conversational reply
        return ChatResponse(
            success=True,
            reply=raw_text,
            action=None,
            corrections=[],
            suggestions=[],
            conversationId=request.conversationId,
            mode=request.mode,
            isFallback=False,
            provider="groq",
            fallbackUsed=True
        )

    reply = data.get("reply", "")
    if not reply:
        reply = raw_text

    action = None
    action_data = data.get("action")
    if isinstance(action_data, dict) and action_data.get("intent"):
        action = StructuredAction(
            intent=action_data.get("intent"),
            appName=action_data.get("appName"),
            parameters=action_data.get("parameters", {}),
            requiresConfirmation=action_data.get("requiresConfirmation", False)
        )

    corrections_data = data.get("corrections", [])
    corrections = [
        CorrectionItem(
            original=c.get("original", ""),
            corrected=c.get("corrected", ""),
            explanation=c.get("explanation", ""),
            rule=c.get("rule")
        )
        for c in corrections_data
        if isinstance(c, dict) and c.get("original") and c.get("corrected")
    ]

    suggestions = [str(s) for s in data.get("suggestions", []) if s]

    return ChatResponse(
        success=True,
        reply=reply,
        action=action,
        corrections=corrections,
        suggestions=suggestions,
        conversationId=request.conversationId,
        mode=request.mode,
        isFallback=False,
        provider="groq",
        fallbackUsed=True
    )
