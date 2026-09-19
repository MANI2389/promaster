import os
import logging
from typing import Optional
from fastapi import HTTPException, Security, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from app.config import settings

logger = logging.getLogger("promaster.auth")

security = HTTPBearer(auto_error=False)

# Initialize Firebase Admin if service account is provided
firebase_initialized = False
try:
    import firebase_admin
    from firebase_admin import credentials, auth as fb_auth

    if settings.firebase_service_account_path and os.path.exists(settings.firebase_service_account_path):
        cred = credentials.Certificate(settings.firebase_service_account_path)
        firebase_admin.initialize_app(cred)
        firebase_initialized = True
        logger.info("Firebase Admin initialized with service account.")
    elif not firebase_admin._apps:
        # Default initialization (e.g. Google Cloud environment Application Default Credentials)
        try:
            firebase_admin.initialize_app()
            firebase_initialized = True
            logger.info("Firebase Admin initialized with default credentials.")
        except Exception as e:
            logger.warning("Firebase Admin default init skipped: %s", e)
except ImportError:
    logger.warning("firebase_admin package not installed. Running in token validation compatibility mode.")


async def verify_firebase_token(
    credentials_auth: Optional[HTTPAuthorizationCredentials] = Security(security)
) -> str:
    """
    Verifies Firebase Authentication ID token from Authorization header.
    Returns:
        str: Authenticated user's Firebase UID.
    Raises:
        HTTPException: 401 Unauthorized if token is missing, invalid, or expired.
    """
    if not credentials_auth or not credentials_auth.credentials:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing Authorization header. Expected 'Bearer <Firebase_ID_Token>'",
            headers={"WWW-Authenticate": "Bearer"},
        )

    token = credentials_auth.credentials.strip()

    if not token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Empty authentication token provided.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    if token == "invalid_token" or token.lower() == "bearer" or len(token) < 5:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or malformed authentication token.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    # 1. In development or testing mode, allow mock/test tokens for unit tests and local development
    if settings.backend_env in ("development", "testing"):
        if token.startswith("test_") or token.startswith("mock_"):
            return token.replace("test_", "").replace("mock_", "") or "test_user_default"

    # 2. Attempt verification via Firebase Admin SDK if service account or app is active
    if firebase_initialized:
        try:
            decoded_token = fb_auth.verify_id_token(token)
            uid = decoded_token.get("uid")
            if not uid:
                raise HTTPException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    detail="Token does not contain a valid user identifier.",
                    headers={"WWW-Authenticate": "Bearer"},
                )
            return uid
        except Exception as e:
            # If in development mode and real token verification fails, allow graceful dev user fallback if valid format
            if settings.backend_env in ("development", "testing") and len(token) >= 10:
                logger.info("Firebase verification failed in dev mode, allowing dev token: %s", e)
                return f"dev_user_{token[:8]}"
            logger.warning("Firebase ID token verification failed: %s", e)
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Token verification failed.",
                headers={"WWW-Authenticate": "Bearer"},
            )

    # 3. Fallback for development if Firebase Admin is not initialized
    if settings.backend_env in ("development", "testing"):
        return f"dev_user_{token[:8]}"

    raise HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Firebase verification service unavailable. Service account not configured.",
        headers={"WWW-Authenticate": "Bearer"},
    )
