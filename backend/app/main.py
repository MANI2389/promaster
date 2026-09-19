import time
import logging
from collections import defaultdict
from typing import Dict, List
from fastapi import FastAPI, Depends, HTTPException, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.config import settings
from app.models import ChatRequest, ChatResponse, HealthResponse
from app.auth import verify_firebase_token
from app.ai_router import route_ai_request
from app.ollama_service import is_ollama_available

# Configure secure logging (never log full messages or secrets)
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger("promaster.main")

app = FastAPI(
    title="PROMASTER AI Backend Gateway",
    description="Secure generative AI proxy for PROMASTER Android application.",
    version="1.0.0"
)

configured_origins = [origin.strip() for origin in settings.cors_allowed_origins.split(",") if origin.strip()]
allowed_origins = configured_origins if settings.backend_env == "production" else (configured_origins or ["*"])

app.add_middleware(
    CORSMiddleware,
    allow_origins=allowed_origins,
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Simple sliding-window rate limiter per user UID
user_request_timestamps: Dict[str, List[float]] = defaultdict(list)


def check_rate_limit(user_id: str):
    """
    Limits requests per user to protect backend and AI quotas.
    """
    now = time.time()
    window_start = now - 60.0  # 1 minute window

    # Clean old timestamps
    timestamps = [t for t in user_request_timestamps[user_id] if t > window_start]
    if len(timestamps) >= settings.rate_limit_per_minute:
        logger.warning("Rate limit exceeded for user: %s", user_id)
        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail="Rate limit exceeded. Please wait a moment before sending another message."
        )

    timestamps.append(now)
    user_request_timestamps[user_id] = timestamps


@app.get("/health", response_model=HealthResponse)
async def health_check():
    """
    Health and readiness probe. Never exposes secrets.
    Reports availability status across configured AI providers.
    """
    ollama_ok = await is_ollama_available()
    return HealthResponse(
        status="healthy",
        environment=settings.backend_env,
        geminiConfigured=bool(settings.gemini_api_key),
        groqConfigured=bool(settings.groq_api_key),
        ollamaConfigured=ollama_ok,
        model=settings.gemini_model
    )


@app.post("/v1/ai/chat", response_model=ChatResponse)
async def chat_endpoint(
    request: ChatRequest,
    user_id: str = Depends(verify_firebase_token)
):
    """
    Authenticated AI chat endpoint for PROMASTER.
    Verifies Firebase Authentication ID token before routing request
    through the bounded multi-provider AI fallback pipeline.
    """
    # Enforce user rate limiting
    check_rate_limit(user_id)

    logger.info(
        "Processing AI chat request. user_id=%s, mode=%s, targetLanguage=%s, msg_len=%d",
        user_id,
        request.mode,
        request.targetLanguage,
        len(request.message)
    )

    try:
        response = await route_ai_request(request)
        return response
    except HTTPException:
        raise
    except Exception as e:
        logger.error("Unexpected error in chat_endpoint: %s", str(e))
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="AI service is temporarily unavailable. Please try again later."
        )


@app.exception_handler(HTTPException)
async def http_exception_handler(request: Request, exc: HTTPException):
    """
    Sanitized HTTP exception handler preventing sensitive data leakage.
    """
    return JSONResponse(
        status_code=exc.status_code,
        content={"detail": exc.detail},
        headers=getattr(exc, "headers", None)
    )
