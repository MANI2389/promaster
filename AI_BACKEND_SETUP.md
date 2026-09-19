# PROMASTER AI Backend Setup Guide

This guide documents the architecture, configuration, local execution, and deployment of the **PROMASTER Secure AI Backend Gateway**.

---

## 1. Architecture Overview

To protect sensitive AI provider credentials (such as Google Gemini API keys) and enforce user authentication, the PROMASTER Android app never communicates directly with third-party LLM providers.

```
┌─────────────────────────┐
│   PROMASTER Android     │
│       Client App        │
└───────────┬─────────────┘
            │
            │ 1. Attach Firebase User ID Token (Bearer Token)
            ▼
┌─────────────────────────┐
│ Secure AI Backend Proxy │  ◄──  Verifies Firebase ID Token
│  (Python 3.14+ FastAPI) │  ◄──  Enforces Rate Limiting (60 req/min)
└───────────┬─────────────┘
            │
            │ 2. Server-side prompt dispatch with secret key
            ▼
┌─────────────────────────┐
│     Gemini 2.5 Flash    │
│      Google GenAI       │
└─────────────────────────┘
```

### Security Principles
- **Zero Client-Side Secrets**: No Gemini, OpenAI, Claude, or Firebase Admin service account keys are stored in the Android APK, resources, Kotlin code, or BuildConfig.
- **Strict Token Verification**: Every request to `POST /v1/ai/chat` requires a valid Firebase Authentication ID token. The server extracts the authenticated `uid` and never trusts arbitrary client-supplied IDs.
- **Rate Limiting & Safety**: Built-in sliding window rate limiter protects quotas and backend resources.
- **Pedagogical Fallback**: If the backend is unreachable or the Gemini API is rate-limited, the system falls back to verified deterministic pedagogical heuristics without breaking client flows.

---

## 2. Directory Structure

```
backend/
├── app/
│   ├── __init__.py
│   ├── config.py           # Environment variables and configuration loader
│   ├── auth.py             # Firebase ID token verification
│   ├── models.py           # Pydantic request/response schemas
│   ├── gemini_service.py   # Google GenAI integration & prompt engineering
│   └── main.py             # FastAPI entrypoint, CORS, rate limiter, routes
├── tests/
│   ├── __init__.py
│   └── test_backend.py     # Comprehensive automated test suite
├── requirements.txt        # Python dependency specifications
├── run.py                  # Convenience local runner script
└── .env.example            # Environment template (safe for source control)
```

---

## 3. Environment Variables

Create a `.env` file inside the `backend/` directory by copying `.env.example`:

```bash
cd backend
copy .env.example .env
```

Configure the following variables:

| Variable | Description | Example / Default |
| :--- | :--- | :--- |
| `GEMINI_API_KEY` | Secret API key from Google AI Studio | `AIzaSy...` (Secret) |
| `FIREBASE_SERVICE_ACCOUNT_PATH` | Path to Firebase Admin service account JSON | `C:/keys/promaster-service-account.json` |
| `FIREBASE_PROJECT_ID` | Firebase project ID | `promaster-ai-core` |
| `BACKEND_ENV` | Environment mode (`development`, `staging`, `production`) | `development` |
| `HOST` | Bind address | `0.0.0.0` |
| `PORT` | Listening port | `8000` |
| `RATE_LIMIT_PER_MINUTE` | Max requests per minute per user UID | `60` |

> [!CAUTION]
> Never commit `.env` or Firebase service account JSON files to Git. Keep them in your local environment or secure secret manager.

---

## 4. Local Setup & Installation

### Prerequisites
- Python 3.10+ (Tested on Python 3.14)
- `pip` package manager

### Step 1: Install Dependencies
```bash
cd backend
pip install -r requirements.txt
```

### Step 2: Run Unit & Integration Tests
```bash
python -m pytest tests/test_backend.py -v
```

All 8 tests should pass, validating:
- Health endpoint status
- Missing token 401 rejection
- Invalid token 401 rejection
- Request validation (422)
- Multi-mode chat generation (Grammar, Speaking, Friend, Coach)
- Rate limiting protection

### Step 3: Start the Backend Server
```bash
python run.py
```
Or with Uvicorn directly:
```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

Server will start on: `http://localhost:8000` for local development only.
Swagger API Documentation: `http://localhost:8000/docs`

---

## 5. Connecting the Android App

### For Android Studio Emulator (debug builds only)
The Android emulator accesses your host machine's `localhost` via the special alias:
```
http://10.0.2.2:8000
```
Use this only as an explicit debug gateway override. `SecureBackendAiClient.DEFAULT_GATEWAY_URL` is the production HTTPS gateway so release builds do not target a local machine.

### For Physical Android Devices (Same Wi-Fi Network)
1. Find your development machine's local IP (e.g. `192.168.1.50` on Windows using `ipconfig`).
2. Pass the custom gateway URL when initializing `SecureBackendAiClient`:
   ```kotlin
   val aiClient = SecureBackendAiClient(backendGatewayUrl = "http://192.168.1.50:8000")
   ```
3. Ensure Windows Firewall allows incoming connections on port `8000`.

---

## 6. API Specification

### `POST /v1/ai/chat`

**Headers:**
```http
Authorization: Bearer <Firebase_ID_Token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "message": "I am go to college yesterday.",
  "conversationId": "conv_user_123",
  "targetLanguage": "English",
  "motherTongue": "Tamil",
  "level": "Beginner",
  "mode": "Grammar Correction"
}
```

**Response Body (200 OK):**
```json
{
  "success": true,
  "reply": "Here is the correction for your sentence:\n\nCorrect: \"I went to college yesterday.\"\n\nExplanation: In English, actions completed in the past require the simple past tense 'went', not 'am go'.",
  "corrections": [
    {
      "original": "I am go to college yesterday.",
      "corrected": "I went to college yesterday.",
      "explanation": "In English, actions completed in the past require the simple past tense 'went', not 'am go'.",
      "rule": "Simple Past Tense"
    }
  ],
  "suggestions": [
    "I went to college yesterday.",
    "Why is 'went' used here?",
    "Give me another past tense practice sentence"
  ],
  "conversationId": "conv_user_123",
  "mode": "Grammar Correction",
  "isFallback": false
}
```

### Supported Learning Modes
- `AI Friend`: Casual, supportive conversational dialogue.
- `Language Coach`: Conceptual explanations and interactive scenarios.
- `Grammar Correction`: Sentence checking with explicit rule explanation and corrected phrasing.
- `Speaking Feedback`: Rhythm, phonetic tips, and pacing guidance.
- `Vocabulary Help`: Definitions, example usage, and memory associations.
- `Translation`: Culturally accurate translation with mother tongue context.
- `General Conversation`: Free-form conversational exchanges.

---

## 7. Cloud Deployment (Cloud Run / Docker)

### Containerfile Example
```dockerfile
FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

### Deploy to Google Cloud Run
```bash
gcloud run deploy promaster-ai-backend \
  --source . \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars BACKEND_ENV=production,FIREBASE_PROJECT_ID=promaster-ai-core \
  --set-secrets GEMINI_API_KEY=promaster-gemini-key:latest
```
