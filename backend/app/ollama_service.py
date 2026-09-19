import json
import logging
import httpx
from typing import Optional, List
from app.config import settings
from app.models import ChatRequest, ChatResponse, CorrectionItem, StructuredAction
from app.gemini_service import build_system_instruction

logger = logging.getLogger("promaster.ollama")


async def is_ollama_available() -> bool:
    """
    Non-blocking health probe to check if Ollama daemon is reachable.
    """
    try:
        url = f"{settings.ollama_base_url.rstrip('/')}/api/tags"
        async with httpx.AsyncClient(timeout=2.0) as client:
            resp = await client.get(url)
            return resp.status_code == 200
    except Exception:
        return False


async def generate_ollama_response(request: ChatRequest) -> ChatResponse:
    """
    Executes local inference through an Ollama daemon if reachable.
    Fails quickly and gracefully with NOT_AVAILABLE when Ollama is offline.
    """
    base_url = settings.ollama_base_url.rstrip('/')
    chat_url = f"{base_url}/api/chat"

    system_instruction = build_system_instruction(
        mode=request.mode,
        target_lang=request.targetLanguage,
        mother_tongue=request.motherTongue,
        level=request.level,
        locale=request.locale
    )

    messages = [{"role": "system", "content": system_instruction}]

    if request.history:
        for turn in request.history[-4:]:
            role = turn.get("role", "user")
            content = turn.get("content", "")
            if content and role in ("user", "assistant"):
                messages.append({"role": role, "content": content[:500]})

    messages.append({"role": "user", "content": request.message})

    payload = {
        "model": settings.ollama_model,
        "messages": messages,
        "stream": False,
        "format": "json"
    }

    try:
        async with httpx.AsyncClient(timeout=settings.ollama_timeout_seconds) as client:
            response = await client.post(chat_url, json=payload)

        if response.status_code == 200:
            data = response.json()
            raw_content = data.get("message", {}).get("content", "")
            return _parse_ollama_response(request, raw_content)
        elif response.status_code == 404:
            logger.warning("Ollama model '%s' not found (404)", settings.ollama_model)
            return ChatResponse(
                success=False,
                reply=f"Ollama model '{settings.ollama_model}' is not available.",
                errorCode="NOT_AVAILABLE",
                conversationId=request.conversationId,
                mode=request.mode,
                isFallback=False,
                provider="ollama",
                fallbackUsed=True
            )
        else:
            logger.warning("Ollama daemon returned HTTP %d", response.status_code)
            return ChatResponse(
                success=False,
                reply="Ollama provider error.",
                errorCode="NOT_AVAILABLE",
                conversationId=request.conversationId,
                mode=request.mode,
                isFallback=False,
                provider="ollama",
                fallbackUsed=True
            )

    except (httpx.ConnectError, httpx.ConnectTimeout) as ce:
        logger.info("Ollama is not running locally: %s", ce)
        return ChatResponse(
            success=False,
            reply="Local Ollama instance is not reachable.",
            errorCode="NOT_AVAILABLE",
            conversationId=request.conversationId,
            mode=request.mode,
            isFallback=False,
            provider="ollama",
            fallbackUsed=True
        )
    except httpx.TimeoutException as te:
        logger.warning("Ollama inference timed out: %s", te)
        return ChatResponse(
            success=False,
            reply="Ollama request timed out.",
            errorCode="TIMEOUT",
            conversationId=request.conversationId,
            mode=request.mode,
            isFallback=False,
            provider="ollama",
            fallbackUsed=True
        )
    except Exception as e:
        logger.info("Ollama request failed: %s", e)
        return ChatResponse(
            success=False,
            reply="Ollama provider failed.",
            errorCode="NOT_AVAILABLE",
            conversationId=request.conversationId,
            mode=request.mode,
            isFallback=False,
            provider="ollama",
            fallbackUsed=True
        )


def _parse_ollama_response(request: ChatRequest, raw_text: str) -> ChatResponse:
    """
    Parses structured or text output from Ollama.
    """
    cleaned = raw_text.strip()
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
        return ChatResponse(
            success=True,
            reply=raw_text,
            action=None,
            corrections=[],
            suggestions=[],
            conversationId=request.conversationId,
            mode=request.mode,
            isFallback=False,
            provider="ollama",
            fallbackUsed=True
        )

    reply = data.get("reply", "") or raw_text

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
        provider="ollama",
        fallbackUsed=True
    )
