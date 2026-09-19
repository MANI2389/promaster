import logging
from app.config import settings
from app.models import ChatRequest, ChatResponse
from app.gemini_service import generate_ai_response as generate_gemini_response, _generate_local_pedagogical_fallback
from app.groq_service import generate_groq_response
from app.ollama_service import generate_ollama_response

logger = logging.getLogger("promaster.ai_router")


async def route_ai_request(request: ChatRequest) -> ChatResponse:
    """
    Resilient Multi-Provider AI Router with Bounded Fallback.
    Provider Execution Priority:
      1. Gemini (Default Cloud Provider: gemini-2.5-flash-lite)
      2. Groq (High-Speed LLM Fallback: llama-3.3-70b-versatile)
      3. Ollama (Optional Local Private AI: llama3.2)
      4. Offline (Deterministic Rule-Based Pedagogical Engine)

    Enforces strictly bounded attempts (maximum 1 attempt per configured provider).
    Never enters endless loops or infinite retries.
    """
    # -------------------------------------------------------------
    # Priority 1: Official Gemini API
    # -------------------------------------------------------------
    if settings.gemini_api_key and settings.gemini_api_key.strip():
        try:
            logger.info("Routing request to Priority 1: Gemini (%s)", settings.gemini_model)
            gemini_res = await generate_gemini_response(request)
            if gemini_res.success:
                gemini_res.provider = "gemini"
                gemini_res.fallbackUsed = False
                return gemini_res
            else:
                logger.warning(
                    "Gemini attempt unsuccessful (errorCode=%s). Initiating fallback cascade...",
                    gemini_res.errorCode
                )
        except Exception as e:
            logger.warning("Gemini execution encountered exception: %s. Continuing to fallback...", e)
    else:
        logger.info("Gemini API key not configured. Proceeding to Priority 2: Groq.")

    # -------------------------------------------------------------
    # Priority 2: Groq Cloud Provider
    # -------------------------------------------------------------
    if settings.groq_api_key and settings.groq_api_key.strip():
        try:
            logger.info("Routing request to Priority 2: Groq (%s)", settings.groq_model)
            groq_res = await generate_groq_response(request)
            if groq_res.success:
                groq_res.provider = "groq"
                groq_res.fallbackUsed = True
                return groq_res
            else:
                logger.warning(
                    "Groq attempt unsuccessful (errorCode=%s). Continuing fallback cascade...",
                    groq_res.errorCode
                )
        except Exception as e:
            logger.warning("Groq execution encountered exception: %s. Continuing to fallback...", e)
    else:
        logger.info("Groq API key not configured. Proceeding to Priority 3: Ollama.")

    # -------------------------------------------------------------
    # Priority 3: Ollama Local Provider (Optional)
    # -------------------------------------------------------------
    try:
        from app.ollama_service import is_ollama_available
        if await is_ollama_available():
            logger.info("Probing Priority 3: Ollama Local (%s at %s)", settings.ollama_model, settings.ollama_base_url)
            ollama_res = await generate_ollama_response(request)
            if ollama_res.success:
                ollama_res.provider = "ollama"
                ollama_res.fallbackUsed = True
                return ollama_res
            else:
                logger.info(
                    "Ollama local instance not available (errorCode=%s). Cascading to Offline engine.",
                    ollama_res.errorCode
                )
        else:
            logger.info("Ollama daemon is not running locally. Cascading to Offline engine.")
    except Exception as e:
        logger.info("Ollama probe skipped or failed: %s. Proceeding to Offline engine.", e)

    # -------------------------------------------------------------
    # Priority 4: Offline Pedagogical & Mobile Action Engine
    # -------------------------------------------------------------
    logger.info("Engaging Priority 4: Deterministic Offline Pedagogical & Automation Engine.")
    offline_res = _generate_local_pedagogical_fallback(request, fallback_reason="All cloud/local AI providers unavailable")
    offline_res.provider = "offline"
    offline_res.fallbackUsed = True
    offline_res.isFallback = True
    offline_res.success = True
    return offline_res
