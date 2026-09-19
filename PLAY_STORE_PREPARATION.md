# PROMASTER — Google Play Store Preparation Guide

This document prepares all required assets, metadata, questionnaires, and signing procedures for publishing **PROMASTER** to the Google Play Store via Google Play Console.

---

## 1. Application Release Metadata

* **Application ID**: `com.example.promaster`
* **Version Code**: `1`
* **Version Name**: `1.0`
* **Target SDK**: `36` (Android 16 / Android 15 compatibility)
* **Minimum SDK**: `24` (Android 7.0 Nougat)
* **Generated Bundle**: `app/build/outputs/bundle/release/app-release.aab`
* **Category**: Education / Language Learning
* **Content Rating**: Everyone (IARC 3+)

---

## 2. Store Copy & Listing Text

### App Title (30 char max)
```text
PROMASTER - AI Language Coach
```

### Short Description (80 char max)
```text
Master languages with your personal AI friend, voice coach & 30-day curriculum.
```

### Full Description (4,000 char max)
See [play_store_listing.md](file:///d:/promaster/play_store_listing.md) for the complete, formatted store copy.

---

## 3. Data Safety Form Declarations

When completing the Google Play Console **Data Safety** questionnaire, submit the following verified declarations:

### A. Data Collection & Sharing
* **Does your app collect or share user data?**: Yes (for core account authentication and educational progress).
* **Is all user data encrypted in transit?**: Yes (all communication uses TLS 1.3 / HTTPS).
* **Can users request their data be deleted?**: Yes (through Firebase account deletion).

### B. Specific Data Types
1. **User Account Info (Email, User ID)**:
   * Collected: Yes
   * Shared: No
   * Purpose: App functionality, Account management
   * Ephemeral: No (stored in Firebase Authentication)
2. **Audio Data (Microphone input for Speech Recognition)**:
   * Collected: Processed in memory on device; NOT stored or collected to servers.
   * Shared: No
   * Purpose: App functionality (speech recognition & pronunciation feedback)
   * Ephemeral: Yes (discarded immediately after processing)
3. **Messages & Learning Content**:
   * Collected: Yes (saved user prompts and progress)
   * Shared: Processed by server-side AI provider (Gemini/Groq) for generating educational feedback.
   * Purpose: App functionality (AI language coaching)

---

## 4. Android Runtime Permissions & Justifications

| Permission | Declaration / Purpose | User-Facing Consent |
| :--- | :--- | :--- |
| `android.permission.RECORD_AUDIO` | Required for speaking practice, pronunciation analysis, and voice conversations. | Prompted at runtime when user taps the microphone button. |
| `android.permission.POST_NOTIFICATIONS` | Required for scheduled daily lesson reminders and streak saver alerts. | Prompted on Android 13+ (API 33+) when notifications are enabled. |
| `android.permission.SCHEDULE_EXACT_ALARM` | Required for reliable daily practice reminder timing. | System settings permission screen when configured by user. |
| `android.permission.INTERNET` | Required for connecting to the secure AI backend and Firebase services. | Normal permission; granted at installation. |

---

## 5. Release Keystore & Play App Signing

The repository is configured to produce signed release artifacts when environment variables are set:

```bash
# Set in your secure local terminal (do not hardcode or commit):
$env:PROMASTER_KEYSTORE_FILE="D:\path\to\your\release.keystore"
$env:PROMASTER_KEYSTORE_PASSWORD="your-keystore-password"
$env:PROMASTER_KEY_ALIAS="your-key-alias"
$env:PROMASTER_KEY_PASSWORD="your-key-password"

# Re-run release build:
.\gradlew.bat bundleRelease
```

Google Play App Signing will manage the app signing key in production.

---

## 6. Graphic Asset Deliverables Checklist

- [ ] **App Icon**: 512 x 512 px, 32-bit PNG with alpha.
- [ ] **Feature Graphic**: 1,024 x 500 px, JPEG or 24-bit PNG without alpha.
- [ ] **Phone Screenshots (Min 4)**: 1080 x 1920 px or 1080 x 2400 px:
  1. *Screenshot 1*: Home Screen (30-Day Plan & Streak Counter)
  2. *Screenshot 2*: AI Friend Chat ("Bro" persona & multilingual practice)
  3. *Screenshot 3*: Real-Time Voice Practice & Pronunciation Feedback
  4. *Screenshot 4*: Grammar Correction & Vocabulary Drills
- [ ] **Tablet Screenshots (Optional but recommended)**: 7-inch and 10-inch layout captures.

---

## 7. Pre-Submission Verification Gate

1. Verify `PRIVACY_POLICY.md` is hosted on a public HTTPS URL (e.g. `https://promaster.app/privacy`).
2. Verify support email address is monitored (e.g. `support@promaster.app`).
3. Deploy Cloud Firestore `(default)` database in Firebase Console.
4. Deploy `firestore.rules` via Firebase CLI.
5. Deploy FastAPI gateway to public HTTPS server.
6. Build and sign release AAB.
7. Upload signed AAB to Google Play Console Internal Testing track.
