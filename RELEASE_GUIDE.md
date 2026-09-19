# PROMASTER — Direct APK Release Guide

This concise guide outlines the step-by-step process for building, signing, hosting, and publishing new direct APK versions of **PROMASTER** via GitHub Releases and GitHub Pages without Google Play Console.

---

## 1. Update Version in `app/build.gradle.kts`
Increment `versionCode` and update `versionName`:
```kotlin
defaultConfig {
    applicationId = "com.example.promaster"
    minSdk = 24
    targetSdk = 36
    versionCode = 2       // e.g., increment from 1 to 2
    versionName = "1.1"   // e.g., update from "1.0" to "1.1"
}
```
Update `release-info.json` and `docs/release-info.json` to match the new version and file size.

---

## 2. Build the Production Release APK
Run the clean build and release assembly:
```powershell
.\gradlew.bat clean
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleRelease
```
The output APK is generated at:
```text
app/build/outputs/apk/release/app-release.apk
```

---

## 3. Verify Signature
Verify the generated APK with Android SDK `apksigner`:
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\apksigner.bat" verify --verbose "app/build/outputs/apk/release/app-release.apk"
```
**Expected Output**:
```text
Verifies
Verified using v2 scheme (APK Signature Scheme v2): true
Number of signers: 1
```

---

## 4. Prepare Release Asset
Rename the verified APK to the standard distribution filename:
```powershell
Copy-Item "app/build/outputs/apk/release/app-release.apk" -Destination "PROMASTER-v1.1.apk"
```

---

## 5. Create GitHub Release
1. Push your latest code to GitHub:
   ```bash
   git add .
   git commit -m "Release PROMASTER v1.1"
   git push origin main
   ```
2. Navigate to your GitHub repository:
   ```text
   https://github.com/<your-username>/promaster/releases/new
   ```
3. Create Tag: `v1.1`
4. Release Title: `PROMASTER 1.1`
5. Release Description:
   ```markdown
   ## PROMASTER Android Release v1.1
   * **Package**: `com.example.promaster`
   * **Version**: `1.1` (Code `2`)
   * **Min Android**: `Android 7.0+`
   
   ### Highlights:
   * AI Friend & Conversational Coach (Gemini + Groq)
   * Real-Time Voice Assistant & Pronunciation Feedback
   * Instant Grammar Correction & Explanations
   * Structured 30-Day Adaptive Curriculum
   * Deterministic Offline Mode & In-App Automations
   
   ### Installation:
   1. Download `PROMASTER-v1.1.apk` below.
   2. Open the file on your Android device.
   3. Allow installation from this source if prompted.
   4. Tap Install and launch PROMASTER!
   ```
6. **Attach Binary**: Drag and drop `PROMASTER-v1.1.apk` into the release binaries section.
7. Click **Publish release**.

---

## 6. GitHub Pages Free Download Website
The download page in `/docs/` automatically resolves the latest release:
1. Go to your GitHub Repository **Settings** -> **Pages**.
2. Under **Build and deployment**:
   * **Source**: `Deploy from a branch`
   * **Branch**: `main`
   * **Folder**: `/docs`
3. Click **Save**.
4. Your download website is live at:
   ```text
   https://<your-username>.github.io/promaster/
   ```
   Users can visit this link on their Android phones and tap **DOWNLOAD PROMASTER APK** directly.

---

## 7. Device Installation & Verification Checklist
Before public announcement, install the APK on a physical test phone:
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "PROMASTER-v1.1.apk"
```
Verify:
- [ ] App launches into Splash screen and transitions cleanly to Auth/Home
- [ ] User streak and XP persist across restarts
- [ ] AI Friend generates responses
- [ ] SpeechRecognizer voice input activates
- [ ] TTS speech output sounds natural
- [ ] Grammar correction parses input accurately
