# PROMASTER Release Checklist

This checklist records the current local verification state. It does not replace Firebase Console, Google Cloud, Play Console, or physical-device verification.

## Automated Local Verification

- Android unit tests: run `gradlew.bat testDebugUnitTest`.
- Debug build: run `gradlew.bat assembleDebug`.
- Release bundle: run `gradlew.bat bundleRelease` after a release keystore is configured.
- Backend tests: run `python -m pytest backend/tests -q`.
- Firestore and Storage rules: review and deploy `firestore.rules` and `storage.rules` from the Firebase project.
- Secret scan: confirm no `backend/.env`, service-account JSON, keystore, or production secret is tracked or packaged.

## Manual Action Required

- RELEASE KEYSTORE: Set `PROMASTER_KEYSTORE_FILE`, `PROMASTER_KEYSTORE_PASSWORD`, `PROMASTER_KEY_ALIAS`, and `PROMASTER_KEY_PASSWORD` in a protected build environment. The release build never falls back to the debug key.
- CLOUD DEPLOYMENT: Deploy the FastAPI backend behind HTTPS, configure Firebase Admin credentials through a secret manager, configure `GEMINI_API_KEY`, set `BACKEND_ENV=production`, and set `CORS_ALLOWED_ORIGINS` only for required web origins.
- FIREBASE CONSOLE: Verify Authentication providers, Firestore rules, Storage rules, release SHA-256, quotas, indexes, and any FCM configuration.
- PRODUCTION GATEWAY: Confirm `https://ai.promaster.app` resolves to the deployed backend, or provide a different HTTPS gateway through the app configuration before release.
- PLAY CONSOLE: Complete app signing, Data Safety, content rating, privacy-policy URL, target API review, screenshots, feature graphic, and internal/closed testing.
- SUPPORT CONTACT: Publish a monitored privacy/support email and a real account-deletion process.
- PHYSICAL DEVICE: Test sign-in, onboarding, learning persistence, microphone permission, speech recognition, notifications, alarms, automation, offline recovery, and app restart.

## Known Limitations

- The Hey Bro on-device model is not bundled. The feature remains unavailable and must not be advertised as active.
- A real Gemini response, cloud deployment, Firebase Console configuration, notification delivery, signing, and Play Store submission cannot be verified from this local workspace alone.
- The application contains local deterministic fallback behavior for selected AI flows; fallback must remain distinguishable from Gemini in user-facing product claims and backend responses.
