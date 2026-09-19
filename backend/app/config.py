import os
from pathlib import Path
from pydantic import BaseModel

def _load_env_file():
    """
    Loads environment variables from backend/.env or .env if present.
    Does not overwrite existing environment variables.
    """
    # 1. Try python-dotenv if installed
    try:
        from dotenv import load_dotenv
        env_path = Path(__file__).resolve().parent.parent / ".env"
        if env_path.exists():
            load_dotenv(dotenv_path=env_path)
            return
    except ImportError:
        pass

    # 2. Native zero-dependency fallback loader
    candidates = [
        Path(__file__).resolve().parent.parent / ".env",  # backend/.env
        Path.cwd() / "backend" / ".env",
        Path.cwd() / ".env",
    ]
    for env_file in candidates:
        if env_file.is_file():
            try:
                with open(env_file, "r", encoding="utf-8") as f:
                    for line in f:
                        line = line.strip()
                        if line and not line.startswith("#") and "=" in line:
                            k, v = line.split("=", 1)
                            k = k.strip()
                            v = v.strip().strip("'\"")
                            if k and k not in os.environ:
                                os.environ[k] = v
                break
            except Exception:
                pass

_load_env_file()

class Settings(BaseModel):
    gemini_api_key: str = os.getenv("GEMINI_API_KEY", "")
    gemini_model: str = os.getenv("GEMINI_MODEL", "gemini-2.5-flash-lite")
    firebase_service_account_path: str = os.getenv("FIREBASE_SERVICE_ACCOUNT_PATH", "")
    firebase_project_id: str = os.getenv("FIREBASE_PROJECT_ID", "promaster-ai-core")
    backend_env: str = os.getenv("BACKEND_ENV", "development")
    host: str = os.getenv("HOST", "0.0.0.0")
    port: int = int(os.getenv("PORT", "8000"))
    rate_limit_per_minute: int = int(os.getenv("RATE_LIMIT_PER_MINUTE", "15"))
    max_input_length: int = int(os.getenv("MAX_INPUT_LENGTH", "1500"))
    max_output_tokens: int = int(os.getenv("MAX_OUTPUT_TOKENS", "1024"))
    gemini_timeout_seconds: float = float(os.getenv("GEMINI_TIMEOUT_SECONDS", "15.0"))
    groq_api_key: str = os.getenv("GROQ_API_KEY", "")
    groq_model: str = os.getenv("GROQ_MODEL", "llama-3.3-70b-versatile")
    groq_timeout_seconds: float = float(os.getenv("GROQ_TIMEOUT_SECONDS", "15.0"))
    ollama_base_url: str = os.getenv("OLLAMA_BASE_URL", "http://127.0.0.1:11434")
    ollama_model: str = os.getenv("OLLAMA_MODEL", "llama3.2")
    ollama_timeout_seconds: float = float(os.getenv("OLLAMA_TIMEOUT_SECONDS", "15.0"))
    cors_allowed_origins: str = os.getenv("CORS_ALLOWED_ORIGINS", "")

settings = Settings()
