# PROMASTER — Autonomous AI Language Coach & Mobile Assistant

PROMASTER is a comprehensive, production-grade Android application and FastAPI gateway designed to take learners to conversational fluency in 30 days through structured pedagogical content, an upbeat AI companion ("bro" persona), real-time speech recognition and synthesis, and safe Android automation.

---

## 🏛️ System Architecture

```
                               ┌─────────────────────────────────────────┐
                               │             PROMASTER APP               │
                               │        (Kotlin / Jetpack Compose)       │
                               └────────────────────┬────────────────────┘
                                                    │
                                                    ▼
                               ┌─────────────────────────────────────────┐
                               │         Secure Backend AI Client        │
                               │        (Firebase Auth Token Auth)       │
                               └────────────────────┬────────────────────┘
                                                    │ HTTPS
                                                    ▼
                               ┌─────────────────────────────────────────┐
                               │           FastAPI Gateway               │
                               │    (Rate Limiter / Token Verifier)      │
                               └────────────────────┬────────────────────┘
                                                    │
                                                    ▼
                               ┌─────────────────────────────────────────┐
                               │             AI Multi-Router             │
                               │   Bounded Cascade Priority Pipeline     │
                               └───┬─────────────┬─────────────┬─────────┘
                                   │             │             │
                    ┌──────────────┘             │             └─────────────┐
                    ▼                            ▼                           ▼
        ┌───────────────────────┐   ┌───────────────────────┐   ┌───────────────────────┐
        │     Priority 1:       │   │      Priority 2:      │   │      Priority 3:      │
        │   Official Gemini     │   │      Groq Cloud       │   │     Ollama Local      │
        │(gemini-2.5-flash-lite)│   │(llama-3.3-70b-versatile│  │      (llama3.2)       │
        └───────────────────────┘   └───────────────────────┘   └───────────────────────┘
                    │                            │                           │
                    └──────────────┬─────────────┴───────────────────────────┘
                                   │ (All Failed or Unconfigured)
                                   ▼
                        ┌───────────────────────┐
                        │      Priority 4:      │
                        │ Deterministic Offline │
                        │  Pedagogical Engine   │
                        └───────────────────────┘
```

---

## 🌟 Key Capabilities

### 1. Resilient Multi-Provider AI Routing
* **Priority 1 (Gemini)**: Default free-tier Google Gemini model (`gemini-2.5-flash-lite`) with context truncation (1500 char cap), rate limiting (15 req/min), and 15s timeout.
* **Priority 2 (Groq Fallback)**: High-speed LLM backup via Groq API (`llama-3.3-70b-versatile`). Automatically engages if Gemini experiences quota exhaustion or rate limits.
* **Priority 3 (Ollama Local)**: Optional on-device/local server inference (`llama3.2` at `http://127.0.0.1:11434`). Fast connection probing ensures zero delay if Ollama is not running.
* **Priority 4 (Deterministic Offline Engine)**: Instant rule-based conversational and grammatical responses when no cloud or local AI is reachable.
* **Zero Faking**: All responses return their authoritative `provider` tag (`gemini`, `groq`, `ollama`, or `offline`) and boolean `fallbackUsed`.

### 2. Upbeat Multilingual & Tanglish Companion
* Natural human friend persona ("bro") with warmth, encouragement, and zero robotic textbook tone.
* Native Indian language support across 8 languages: English, Tamil, Tanglish, Hindi, Malayalam, Telugu, Kannada, Bengali.
* Speaks and understands authentic Tanglish (Tamil written in English script) when addressed by the user.

### 3. Voice Assistant & TTS
* **Speech Recognition**: Android `SpeechRecognizer` with MainLooper enforcement, timeout protection, error mapping, and BCP-47 language selection (`ta-IN`, `hi-IN`, `en-US`, etc.).
* **Text-to-Speech**: Male and Female voice selection, pitch, speech rate adjustment, and dynamic matching against real installed Android voice packs with fallback to closest available voice.
* **Hey Bro Wake Word**: Truthful detection state reporting `UNAVAILABLE_NO_ON_DEVICE_MODEL` when an on-device neural model is not bundled, strictly following zero-faking policy.

### 4. Safe Mobile Automation Sandbox
* Structured intent parsing: `OPEN_APP`, `CREATE_ALARM`, `OPEN_SETTINGS`, `START_LANGUAGE_LESSON`, `OPEN_WEBSITE`.
* Supports popular apps: YouTube, WhatsApp, Chrome, Calculator, Maps, Settings, Clock.
* Strict safety enforcement: confirmation required for any sensitive actions; never dials or executes silently.

### 5. Persistent Startup State Machine
* Authoritative state flow: `Splash` -> `Auth Check` -> `Onboarding` or `Home`.
* Uses Firebase Authentication as the single source of truth; never logs out authenticated users on app restart.
* Local `UserPreferencesRepository` caches streak, XP, target language, and level.

---

## 🚀 Getting Started

### Prerequisites
* Android Studio Ladybug / Koala or IntelliJ IDEA with Android SDK 36.
* Java Development Kit (JDK) 17.
* Python 3.10+ (for FastAPI backend).

### Backend Setup
1. Navigate to the backend directory:
   ```bash
   cd backend
   ```
2. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```
3. Copy environment template and configure keys:
   ```bash
   cp .env.example .env
   # Edit .env and supply GEMINI_API_KEY and optionally GROQ_API_KEY
   ```
4. Run backend tests:
   ```bash
   python -m pytest tests -v
   ```
5. Start local server:
   ```bash
   python -m uvicorn app.main:app --host 0.0.0.0 --port 8000
   ```

### Android Build & Verification
1. Run Android unit tests:
   ```bash
   .\gradlew.bat testDebugUnitTest
   ```
2. Build debug APK:
   ```bash
   .\gradlew.bat assembleDebug
   ```
3. Build release APK & AAB:
   ```bash
   .\gradlew.bat assembleRelease bundleRelease
   ```
4. Install to connected device via ADB:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 🔒 Security & Privacy Boundary

* **Zero Secrets in Android Code**: No API keys, passwords, or service account credentials are built into the APK/AAB. All AI provider keys reside exclusively on the server.
* **Per-User Cloud Isolation**: Cloud Firestore security rules enforce strict `request.auth.uid == userId` isolation across all collections (`users`, `learningPlans`, `dailyTasks`, `progress`, `conversations`, `settings`).
* **Privacy-First Audio**: Microphone access operates only during active user engagement; raw audio buffers are processed in memory and discarded without cloud persistence.

---

## 📄 License & Compliance
* See [PRIVACY_POLICY.md](file:///d:/promaster/PRIVACY_POLICY.md) for data safety disclosures.
* See [RELEASE_CHECKLIST.md](file:///d:/promaster/RELEASE_CHECKLIST.md) for production release preparation.
* See [AI_PROVIDER_SETUP.md](file:///d:/promaster/AI_PROVIDER_SETUP.md) for LLM configuration details.
