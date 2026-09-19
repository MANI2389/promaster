# PROMASTER - Google Play Store Listing & Launch Checklist

This document contains the complete production metadata, store copy, visual asset specifications, privacy declarations, and Data Safety disclosures required for submitting **PROMASTER** to the Google Play Console.

---

## 1. Store Listing Metadata

### App Title (Max 30 characters)
```text
PROMASTER - AI Language Coach
```

### Short Description (Max 80 characters)
```text
Master languages with your personal AI friend, voice coach & 30-day curriculum.
```

### Full Description (Max 4,000 characters)
```text
Meet PROMASTER — your all-in-one AI language companion, personal speaking coach, and safe voice assistant designed to help you achieve conversational fluency in 30 days.

Whether you are starting from zero or sharpening your conversation skills, PROMASTER combines structured curriculum with natural interactive AI conversations to build lasting confidence.

==================================================
🌟 KEY FEATURES
==================================================

🤖 1. CONVERSATIONAL AI FRIEND
• Practice real-world dialogue anytime with an empathetic AI friend who never judges your mistakes.
• Conversational roleplays: ordering food in a café, airport check-in, job interviews, or casual daily banter.
• Instant explanations in your mother tongue (Tamil, Hindi, Spanish, and English) to demystify complex grammar.

🎙️ 2. REAL-TIME VOICE ASSISTANT
• Start hands-free practice from the in-app voice assistant when microphone access is granted.
• High-accuracy speech recognition evaluates pronunciation, fluency, and sentence structure.
• The optional "Hey Bro" wake-word control is not currently available until its on-device model is installed.

📚 3. STRUCTURED 30-DAY CURRICULUM
• Bite-sized daily micro-lessons engineered for busy schedules (5 to 15 minutes a day).
• 6 Core Activity Types:
  - Vocabulary Workout with audio pronunciation
  - Grammar Breakdown with clear contextual rules
  - Interactive Listening Dialogues
  - Speaking & Pronunciation Drills
  - Knowledge Reinforcement Quizzes
  - AI Free-Talk Exercises
• Anti-Skipping Sequential Gate: Ensures foundational concepts are mastered before advancing to complex topics.

🔥 4. STREAKS, XP & MOTIVATION
• Earn XP for completing daily tasks, passing quizzes, and maintaining consistency.
• Non-exploitable streak counter tracks continuous daily practice.
• Personalized learning reminders at your preferred daily practice time.

🛡️ 5. SAFE ANDROID AUTOMATION SANDBOX
• Perform useful device actions safely: "Start my English lesson", "Open YouTube", "Set an alarm for 7:00 AM", or "Open settings".
• Strict permission gating: Sensitive actions require explicit confirmation before execution and never dial or automate silently.

==================================================
🔒 PRIVACY-FIRST COMMITMENT
==================================================
• No continuous background audio uploads — audio is only processed when you actively tap the microphone or enable wake-word listening.
• Strict per-user cloud data isolation: Your progress, profile, and chat history are secured with Firebase Cloud Firestore user-ownership security rules.
• Zero private API keys in the app bundle.

==================================================
🌍 SUPPORTED TARGET LANGUAGES
==================================================
English, Spanish, French, German, Japanese, Mandarin, Tamil, Hindi, Arabic, Portuguese, Italian, Korean, Russian, Telugu, Malayalam.

Download PROMASTER today and speak your new language with confidence!
```

---

## 2. Graphic Asset Specifications

| Asset | Dimensions | Format | Max File Size | Status / Notes |
| :--- | :--- | :--- | :--- | :--- |
| **App Icon** | 512 x 512 px | 32-bit PNG (with alpha) | 1,024 KB | Generated from launcher adaptive icon (`@mipmap/ic_launcher`) |
| **Feature Graphic** | 1,024 x 500 px | JPEG or 24-bit PNG (no alpha) | 15 MB | Promotional banner with PROMASTER logo, tagline, and AI orb |
| **Phone Screenshots** | Min 1080 x 1920 px (9:16) | JPEG or 24-bit PNG | 8 MB each | Minimum 2, recommended 4-8 covering: 1. Home / 30-Day Plan, 2. AI Chat / Friend, 3. Speaking / Voice Assistant, 4. Vocabulary / Quiz, 5. Progress & Streaks |
| **7-inch Tablet Screenshots** | Min 1200 x 1920 px | JPEG or 24-bit PNG | 8 MB each | Responsive adaptive layout captures |
| **10-inch Tablet Screenshots** | Min 1600 x 2560 px | JPEG or 24-bit PNG | 8 MB each | Tablet dual-pane layout captures |

---

## 3. Categorization & Contact Details

* **Application Type**: App
* **Category**: Education
* **Tags**: Language Learning, Education, AI Chat, Voice Assistant, Study Tools, Productivity
* **Developer Name**: PROMASTER Learning Inc.
* **Developer Email**: `support@promaster.app`
* **Developer Website**: `https://promaster.app`
* **Privacy Policy URL**: `https://promaster.app/privacy`

---

## 4. Target Audience & Content Rating

* **Target Age Group**: 13 and older (Teens, Young Adults, Adults).
* **Children's Online Privacy Protection Act (COPPA)**: Does **NOT** primarily target children under 13.
* **IARC Content Rating Questionnaire Answers**:
  - Violence: No
  - Sexuality: No
  - Profanity / Crude Humor: No
  - Controlled Substances: No
  - User-Generated Content / Unmoderated Chat: No (Interactions are with an AI language tutor and pre-curated lessons)
  - Shares User Physical Location: No
  - Allows Digital Goods Purchases: Optional subscriptions (if enabled)
  - Rating Outcome: **Everyone / PEGI 3 / USK 0**

---

## 5. Google Play Data Safety Declarations

### Data Collected:
1. **Personal Information**:
   - *Name*: Optional (User profile personalization)
   - *Email Address*: Required for Firebase Authentication account management
   - *User IDs*: Internal Firebase UID for data isolation
2. **Audio Data**:
   - *Voice / Sound Recordings*: Ephemeral audio collected strictly during user-initiated speech practice sessions. Audio is processed for real-time speech-to-text / pronunciation evaluation and is **not stored permanently or sold**.
3. **App Activity**:
   - *App Interactions*: Quiz submissions, completed daily tasks, streaks, and XP points for educational progress tracking.
4. **App Performance**:
   - *Crash logs & Diagnostics*: Anonymized crash diagnostic telemetry (if enabled).

### Security & Privacy Commitments:
- **Data Encryption**: All data is encrypted in transit over secure HTTPS/TLS 1.3.
- **Data Deletion**: Users can request complete account and data deletion directly from the Profile/Settings screen or via email request.
- **No Third-Party Data Brokers**: User data is never shared with third-party advertisers or data brokers.

---

## 6. Play Console Release Deployment Steps

1. **Log in** to [Google Play Console](https://play.google.com/console).
2. Create app entry: `PROMASTER` (Language: English, Free).
3. Complete **App Content Declarations**:
   - Privacy policy link
   - Ads declaration (e.g. No Ads or Contains Ads depending on monetization)
   - App access (Provide credentials for a test/demo account)
   - Content rating (Complete IARC questionnaire)
   - Target audience (13+)
   - Data safety questionnaire (as detailed in Section 5 above)
   - Financial features / Government apps / Health apps: "None"
4. Complete **Main Store Listing**:
   - Paste App Title, Short Description, and Full Description
   - Upload 512x512 Icon and 1024x500 Feature Graphic
   - Upload Phone and Tablet screenshots
5. Go to **Release** > **Testing** > **Internal testing** (or Production).
6. Create new release:
   - Upload signed App Bundle: `app/build/outputs/bundle/release/app-release.aab`
   - Release name: `1.0 (1)`
   - Enter release notes: `Initial release of PROMASTER with 30-day curriculum, AI language coach, and voice practice.`
7. Review and rollout release to internal testers.
