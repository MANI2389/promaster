# PROMASTER — Production Deployment & Cloud Architecture Guide

This guide outlines the production deployment steps for the **PROMASTER AI Gateway Backend** and the accompanying **Firebase Cloud Infrastructure**.

---

## 1. Production Backend Deployment Requirements

### Architecture
* **Protocol**: Strict HTTPS with valid TLS/SSL certificates (Let's Encrypt or Cloud Managed Certificate).
* **Process**: Containerized FastAPI application running via Uvicorn/Gunicorn.
* **Environment**: `BACKEND_ENV=production`.
* **CORS**: Restricted origins configured via `CORS_ALLOWED_ORIGINS`.
* **AI Keys**: Injected securely via environment variables (never baked into the container image).

---

## 2. Docker Container Deployment

### Build the Image
```bash
cd backend
docker build -t promaster-ai-backend:latest .
```

### Run with Production Secrets
```bash
docker run -d \
  --name promaster-gateway \
  -p 8000:8000 \
  -e BACKEND_ENV=production \
  -e GEMINI_API_KEY="your-production-gemini-key" \
  -e GEMINI_MODEL="gemini-2.5-flash-lite" \
  -e GROQ_API_KEY="your-production-groq-key" \
  -e GROQ_MODEL="llama-3.3-70b-versatile" \
  -e RATE_LIMIT_PER_MINUTE=20 \
  -e MAX_INPUT_LENGTH=1500 \
  -e MAX_OUTPUT_TOKENS=1024 \
  promaster-ai-backend:latest
```

---

## 3. Google Cloud Run Deployment (Serverless)

Cloud Run is the recommended production host for the PROMASTER backend gateway:

1. **Submit Build to Google Container Registry / Artifact Registry**:
   ```bash
   gcloud builds submit --tag gcr.io/promaster-ai-core/promaster-backend:latest backend
   ```

2. **Deploy to Cloud Run with Secret Manager**:
   ```bash
   gcloud run deploy promaster-ai-gateway \
     --image gcr.io/promaster-ai-core/promaster-backend:latest \
     --platform managed \
     --region us-central1 \
     --allow-unauthenticated \
     --set-env-vars BACKEND_ENV=production,GEMINI_MODEL=gemini-2.5-flash-lite \
     --set-secrets GEMINI_API_KEY=GEMINI_API_KEY:latest,GROQ_API_KEY=GROQ_API_KEY:latest \
     --min-instances 1 \
     --max-instances 10 \
     --memory 512Mi \
     --cpu 1
   ```

3. **Obtain Production Gateway URL**:
   The command outputs a service URL:
   `https://promaster-ai-gateway-xyz-uc.a.run.app`

---

## 4. Firebase Production Setup

### A. Provision Cloud Firestore Database
1. Open [Google Cloud Console / Datastore Setup](https://console.cloud.google.com/datastore/setup?project=promaster-ai-core).
2. Create the default database `(default)` in Firestore Native mode.
3. Select your preferred multi-region or regional location (e.g., `nam5` or `asia-south1`).

### B. Deploy Security Rules
Deploy `firestore.rules` directly from the project root:
```bash
firebase deploy --only firestore:rules --project promaster-ai-core
```
This guarantees user-isolated access controls on:
* `users/{userId}`
* `learningPlans/{userId}`
* `dailyTasks/{userId}/tasks/{taskId}`
* `progress/{userId}`
* `conversations/{userId}/messages/{messageId}`
* `settings/{userId}`

### C. Deploy Firestore Composite Indexes
```bash
firebase deploy --only firestore:indexes --project promaster-ai-core
```

---

## 5. Connecting Android Application to Production

In `app/src/main/java/com/example/promaster/ai/AIProviderBackendClient.kt`:
Update the backend endpoint or configure it dynamically via `UserPreferencesRepository`:
```kotlin
const val PRODUCTION_BACKEND_URL = "https://your-production-domain.com/v1/ai/chat"
```

### Network Security Config
For production builds, the app uses standard Android TLS verification. Cleartext traffic (`http://`) is disabled for release builds in `AndroidManifest.xml` via:
```xml
android:usesCleartextTraffic="false"
```

---

## 6. Health & Readiness Verification

Run the readiness probe against the deployed production gateway:
```bash
curl -f https://your-production-domain.com/health
```
Expected output:
```json
{
  "status": "healthy",
  "environment": "production",
  "geminiConfigured": true,
  "groqConfigured": true,
  "ollamaConfigured": false,
  "model": "gemini-2.5-flash-lite"
}
```
If the status reports `healthy`, the gateway is live and ready to serve production traffic.
