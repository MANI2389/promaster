import uvicorn
from app.config import settings

if __name__ == "__main__":
    print(f"Starting PROMASTER AI Backend on {settings.host}:{settings.port} ({settings.backend_env} mode)...")
    uvicorn.run(
        "app.main:app",
        host=settings.host,
        port=settings.port,
        reload=True if settings.backend_env == "development" else False
    )
