# NOVA AI — Android app (production-ready project)

Native Android app (Kotlin + Jetpack Compose) with a Firebase backend. Built for phones, not desktop.

- App name: **NOVA AI**
- Application ID: `com.muhammdsamirkabirkhan.novaai`
- Developer: muhammdsamirkabirkhan
- Min SDK 26, Target SDK 36 (meets Google Play's Aug 31 2026 requirement)

## What's in this project

```
app/                  Android app (Kotlin, Jetpack Compose, Material 3)
backend/              Firebase backend: Firestore rules + Cloud Functions (Node 22 / TypeScript)
web/                  Privacy Policy, Terms of Service (host these, e.g. Firebase Hosting)
store-assets/         Play Store icon (512x512) and feature graphic (1024x500)
docs/PUBLISHING_GUIDE.md   Step-by-step: accounts, keys, backend deploy, signing, Play Console
nova.properties.example    Non-secret Android build config template (copy → nova.properties)
keystore.properties.example  Signing config template (copy → keystore.properties)
```

**No AI provider key, Play service-account key, or signing key is committed.** They are loaded from
git-ignored files (`nova.properties`, `keystore.properties`, `backend/functions/.env` + Firebase Secrets)
that you fill in — see `docs/PUBLISHING_GUIDE.md`.

## How the pieces fit together

1. The Android app never calls an AI provider or the Google Play Developer API directly. It only calls
   **your Firebase Cloud Functions** with the signed-in user's ID token.
2. Cloud Functions hold the Anthropic API key (as a Firebase **Secret**, not in code) and the Play
   Console service-account credentials, check/consume the user's daily quota, and are the only thing
   allowed to write `plan`, `premiumUntil`, chats, or messages in Firestore (see `firestore.rules`).
3. Google Play Billing runs the actual purchase UI; the app forwards only the purchase **token** to the
   backend, which verifies it with Google, acknowledges it, and flips the user to Premium.
4. AdMob (banner/interstitial/rewarded) only initializes for **signed-in Free-plan users**, after UMP
   consent. Rewarded-ad credits are granted server-side via AdMob SSV callbacks, not by the client.

## Opening the project (first time)

This project was generated without network access, so the small `gradle-wrapper.jar` binary isn't
included (only the wrapper scripts and config are). To fix that in one step:

1. Open the `NovaAI` folder in **Android Studio** (File → Open).
2. Android Studio will offer to regenerate the missing wrapper automatically; if not, open a terminal
   in the project root and run `gradle wrapper --gradle-version 8.14.3` once (requires Gradle
   installed locally, one-time only) — this creates `gradle/wrapper/gradle-wrapper.jar`.
3. After that, `./gradlew` works normally for every build in this guide.

## Quick start (after completing the accounts in the publishing guide)

```bash
# Backend
cd backend
npm --prefix functions install
firebase functions:secrets:set ANTHROPIC_API_KEY
firebase deploy --only firestore:rules,functions

# Android app
cd ..
cp nova.properties.example nova.properties        # fill in real AdMob IDs + hosted legal URLs
cp keystore.properties.example keystore.properties  # after generating your upload key
# place Firebase's google-services.json into app/
./gradlew bundleRelease     # -> app/build/outputs/bundle/release/app-release.aab
```

Full instructions, including which accounts to create and exactly which keys go where, are in
**`docs/PUBLISHING_GUIDE.md`**.
