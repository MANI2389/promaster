# PROMASTER Privacy Policy

Last updated: 2026-09-19

PROMASTER is a language-learning application with optional AI, microphone, notification, and Android automation features.

## Information Used

- Account information: Firebase Authentication uses the email address and authentication state supplied during account creation or sign-in.
- Learning data: language preferences, lesson progress, vocabulary, quiz results, XP, streak data, and reminder settings are stored for the signed-in Firebase user.
- Conversations: AI conversation requests and saved conversation data may contain the text the user submits.
- Voice data: microphone access is used only for speech recognition, speaking practice, and the optional wake-word feature when enabled. Raw audio is not retained by default.
- Notifications: reminder scheduling uses the device notification and alarm systems. PROMASTER does not claim delivery when notification permission is denied or the operating system suppresses an alarm.

## AI Processing

When the protected AI backend is configured and reachable, submitted text is sent over HTTPS with a Firebase ID token. The backend authenticates the user and may send the request to Google Gemini using a server-side credential. Gemini credentials are not stored in the Android application.

If the backend is unavailable, PROMASTER may use deterministic local pedagogical responses where implemented. Local fallback responses are not represented as live Gemini responses.

## Storage and Security

Firebase Authentication, Cloud Firestore, and Firebase Storage are configured with per-user ownership rules. The application does not intentionally collect passwords, private keys, API keys, or raw microphone recordings. Authentication tokens are used for requests and must not be exposed in logs or user-facing messages.

## Sharing

Text submitted to the AI feature may be processed by the PROMASTER backend and, when configured, Google Gemini. Data is not sold. PROMASTER does not add advertising analytics or third-party tracking beyond services required by the enabled application features.

## Retention and Deletion

Cloud data remains until the user deletes it through the configured account or Firebase data-management process. A production account-deletion workflow and published support contact must be configured before Play Store submission.

## Permissions

PROMASTER may request internet, microphone, notification, alarm, and microphone foreground-service permissions. Permissions are checked before the related feature runs. Denied permissions disable the dependent feature rather than being bypassed.

## Current Limitations

The on-device "Hey Bro" wake-word model is not bundled in the current application. The wake-word service therefore reports `UNAVAILABLE_NO_ON_DEVICE_MODEL` and does not fake detection.

## Contact

Before publication, the developer must replace this section with a monitored support and privacy contact address.
