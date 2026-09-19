# PROMASTER — Multi-Provider AI Setup & Configuration Guide

PROMASTER employs a resilient, bounded 4-tier AI routing architecture:
1. **Google Gemini** (Primary Cloud LLM)
2. **Groq Cloud** (High-Speed Cloud Fallback)
3. **Ollama Local** (Optional Local Offline Inference)
4. **Deterministic Offline Engine** (Zero-Network Pedagogical Fallback)

---

## 🔒 Golden Rule: Zero Secrets on Client Devices

* **NEVER** place `GEMINI_API_KEY` or `GROQ_API_KEY` inside the Android application.
* **NEVER** store keys in Kotlin code, `BuildConfig`, XML resources, or `google-services.json`.
* **NEVER** commit `backend/.env` to Git.
* All AI keys remain server-side in the backend environment.

---

## 1. Provider 1: Google Gemini (Primary)

### Model: `gemini-2.5-flash-lite`
PROMASTER uses `gemini-2.5-flash-lite` as its official default free-tier model, delivering rapid conversational responses with minimal latency and high multilingual proficiency.

### Configuration
In `backend/.env`:
```bash
GEMINI_API_KEY=your_gemini_api_key_here
GEMINI_MODEL=gemini-2.5-flash-lite
GEMINI_TIMEOUT_SECONDS=15.0
```

### Free-Tier Safeguards Implemented
* **Rate Limiting**: 15 requests per minute per user.
* **Context Truncation**: Maximum input length capped at 1,500 characters.
* **Bounded Output**: Output generation capped at 1,024 tokens.
* **Execution Timeout**: 15.0 seconds maximum before initiating fallback.

### Obtaining a Free Key
1. Visit [Google AI Studio](https://aistudio.google.com/).
2. Sign in with your Google account.
3. Click **Get API Key** -> **Create API Key**.
4. Paste the key into `backend/.env` as `GEMINI_API_KEY`.

---

## 2. Provider 2: Groq Cloud (Automatic Fallback)

### Model: `llama-3.3-70b-versatile`
Groq provides blazing-fast inference via custom LPU hardware. When configured, the router automatically cascades to Groq if Gemini hits rate limits (HTTP 429) or transient server errors.

### Configuration
In `backend/.env`:
```bash
GROQ_API_KEY=your_groq_api_key_here
GROQ_MODEL=llama-3.3-70b-versatile
GROQ_TIMEOUT_SECONDS=15.0
```

### Obtaining a Key
1. Visit [Groq Console](https://console.groq.com/).
2. Create an account and generate an API key.
3. Set `GROQ_API_KEY` in `backend/.env`.
*(Note: If `GROQ_API_KEY` is not provided, the router safely skips Groq and checks Ollama).*

---

## 3. Provider 3: Ollama Local (Optional)

### Model: `llama3.2`
Ollama allows completely local, private inference on developer machines or edge servers.

### Configuration
In `backend/.env`:
```bash
OLLAMA_BASE_URL=http://127.0.0.1:11434
OLLAMA_MODEL=llama3.2
OLLAMA_TIMEOUT_SECONDS=15.0
```

### Running Ollama Locally (Optional)
```bash
# Install Ollama from https://ollama.com/
ollama serve

# In another terminal, pull the model:
ollama pull llama3.2
```

### Production Behavior
* Ollama is **100% optional**.
* Fast health checks probe port 11434. If Ollama is not running, the router skips it immediately in <10ms without holding up the user.
* Production cloud deployments do **NOT** depend on developer PCs.

---

## 4. Provider 4: Deterministic Offline Pedagogical Engine

When the device has no internet access or all cloud providers are unreachable:
* The system engages the built-in pedagogical rule engine.
* Supports all 8 modes: Grammar correction, vocabulary breakdowns, translation examples, conversational coaching, and mobile automation parsing.
* Correctly returns:
  ```json
  {
    "success": true,
    "provider": "offline",
    "fallbackUsed": true,
    "isFallback": true
  }
  ```

---

## 5. Standardized Response Format

Every response returned to the Android client follows this unified schema:

```json
{
  "success": true,
  "reply": "Hey bro! Let's practice some English sentences today.",
  "action": {
    "intent": "OPEN_APP",
    "appName": "YouTube",
    "parameters": {},
    "requiresConfirmation": false
  },
  "corrections": [
    {
      "original": "am go to",
      "corrected": "went to",
      "explanation": "Use past tense 'went' for events that happened yesterday.",
      "rule": "Simple Past Tense"
    }
  ],
  "suggestions": ["Ask me a question in English", "Practice past tense"],
  "conversationId": "thread_abc123",
  "mode": "AI_FRIEND",
  "isFallback": false,
  "provider": "gemini",
  "fallbackUsed": false,
  "errorCode": null
}
```

---

## 6. Verifying Provider Status

You can probe the backend health endpoint at any time:
```bash
curl http://localhost:8000/health
```
Response:
```json
{
  "status": "healthy",
  "environment": "development",
  "geminiConfigured": true,
  "groqConfigured": true,
  "ollamaConfigured": false,
  "model": "gemini-2.5-flash-lite"
}
```
Notice that zero credentials, tokens, or keys are exposed in this probe.
