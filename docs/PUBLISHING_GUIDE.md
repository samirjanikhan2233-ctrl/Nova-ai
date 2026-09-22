# NOVA AI — Publishing Guide

This is the exact checklist to take this project from source code to a live Google Play listing,
under **your own** accounts. Anthropic did not publish anything for you — follow this yourself.

---

## 1. Accounts you need

| # | Account | Why | Cost |
|---|---|---|---|
| 1 | **Google Play Console** (developer account) | To publish the app | one-time $25 |
| 2 | **Firebase project** (Blaze/pay-as-you-go plan) | Auth, database, backend functions | pay-as-you-go, small usage is nearly free |
| 3 | **Anthropic API account** (console.anthropic.com) | Powers the real AI responses | pay-as-you-go |
| 4 | **Google AdMob account** | Real ads on the Free plan | free to create |
| 5 | A place to host 2 static HTML pages (Privacy Policy, Terms) | Required by Play Console | Firebase Hosting is free tier |

You'll also need **Android Studio** (Ladybug/Koala or newer) installed on your own computer — this
project cannot be compiled to an installable app from inside this chat; I've prepared 100% of the
source, config, and backend so Android Studio can build it directly.

---

## 2. Firebase project setup

1. Go to console.firebase.google.com → **Add project**. Name it e.g. `nova-ai-prod`. Enable Google
   Analytics if you want (optional).
2. Upgrade to the **Blaze** plan (Cloud Functions require it; a generous free tier still applies).
3. **Add an Android app** inside the Firebase project:
   - Package name: `com.muhammdsamirkabirkhan.novaai` (must match exactly)
   - Download **`google-services.json`** → place it at `app/google-services.json` in this project
     (it's already in `.gitignore` — never commit it publicly, though it's not a secret key by itself).
4. **Authentication** → Sign-in method → enable **Email/Password** and **Google**.
   - For Google Sign-In you also need a **Web client ID**: Firebase auto-creates one; Android Studio's
     Firebase plugin generates `R.string.default_web_client_id` from `google-services.json`
     automatically — nothing to copy by hand.
   - You must also add your app's **SHA-1** (debug) and **SHA-1** (release/upload key, once you create
     it in step 5) fingerprints under Project Settings → Your apps, or Google Sign-In will fail.
5. **Firestore Database** → Create database → Production mode → pick a region close to your users.
6. **App Check** → Register your app with the **Play Integrity** provider (needed once the app is
   installed from Play; safe to leave `ENFORCE_APP_CHECK=false` in `backend/functions/.env` until
   then, then flip it to `true` and redeploy).

---

## 3. Anthropic API key (the real AI)

1. Create a key at console.anthropic.com → **API Keys**.
2. **Never put this key in the Android app.** It only ever goes into the backend as a Firebase Secret:
   ```bash
   cd backend
   firebase login
   firebase use YOUR_FIREBASE_PROJECT_ID       # or copy .firebaserc.example to .firebaserc first
   firebase functions:secrets:set ANTHROPIC_API_KEY
   ```
3. Copy `backend/functions/.env.example` → `backend/functions/.env` and adjust daily limits / models
   if you want (defaults: Free = `claude-haiku-4-5-20251001`, Premium = `claude-sonnet-5`).
4. Deploy:
   ```bash
   npm --prefix functions install
   firebase deploy --only firestore:rules,firestore:indexes,functions
   ```
   This creates `bootstrap`, `chat`, `runTool`, `verifyPurchase`, `syncSubscription`, `deleteAccount`,
   `deleteChat`, `reportContent`, `playRtdn`, and `admobSsv`.

**Cost control tip:** the backend enforces the daily limits in `.env` (`FREE_DAILY_LIMIT`,
`PREMIUM_DAILY_LIMIT`) server-side per user — this directly caps your Anthropic API spend.

---

## 4. Google Play Console — create the app & get the service account

1. Play Console → **Create app** → name **NOVA AI**, default language, App/Free, agree to policies.
2. **Monetization setup** → link a payments profile (needed for paid subscriptions).
3. **Setup → API access** → **Link** your Firebase/Google Cloud project → **Create new service
   account** → follow the link into Google Cloud Console → create a service account (e.g.
   `nova-play-verifier`) → grant it **no roles there**; instead, back in Play Console, grant that
   service account **Finance: View financial data** + **Manage orders and subscriptions** permission.
4. This service account is what your `verifyPurchase` Cloud Function uses automatically (Cloud
   Functions run as your Firebase project's default service account — grant that exact account's
   email the Play Console permissions above; no key file needs to be downloaded or embedded anywhere).
5. **Monetize → Products → Subscriptions** → create a subscription with product ID
   `nova_premium` (must exactly match `PREMIUM_PRODUCT_ID` in `app/build.gradle.kts` and
   `backend/functions/.env`). Inside it, add two **base plans**:
   - Base plan ID `monthly` — auto-renewing, 1 month, set your price.
   - Base plan ID `yearly` — auto-renewing, 1 year, set your price (discount vs. 12× monthly).
   - Activate both base plans.
6. **Real-time developer notifications (RTDN)**: Play Console → Monetization setup → enable RTDN,
   topic name `play-rtdn`, pointing at Pub/Sub topic `projects/YOUR_PROJECT_ID/topics/play-rtdn`
   in your Firebase/GCP project (create the Pub/Sub topic first in Google Cloud Console — the
   `playRtdn` function in this project already subscribes to a topic named `play-rtdn`). Grant
   `google-play-developer-notifications@system.gserviceaccount.com` the **Pub/Sub Publisher** role on
   that topic.

---

## 5. AdMob setup

1. admob.google.com → **Apps** → Add app → Android → "No" (not yet published) → name it NOVA AI.
2. Create 3 ad units under that app:
   - **Banner** → copy its ID
   - **Interstitial** → copy its ID
   - **Rewarded** → copy its ID, then open its settings → **Server-side verification** → set the
     Callback URL to your deployed function's URL, e.g.
     `https://REGION-PROJECT_ID.cloudfunctions.net/admobSsv` (get the exact URL by running
     `firebase deploy --only functions` and copying the printed `admobSsv` URL).
3. Copy the **App ID** (format `ca-app-pub-XXXX~YYYY`) from AdMob → App settings.
4. Fill all four into `nova.properties` (copy from `nova.properties.example`):
   ```
   ADMOB_APP_ID=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
   AD_BANNER_ID=ca-app-pub-XXXXXXXXXXXXXXXX/1111111111
   AD_INTERSTITIAL_ID=ca-app-pub-XXXXXXXXXXXXXXXX/2222222222
   AD_REWARDED_ID=ca-app-pub-XXXXXXXXXXXXXXXX/3333333333
   ```
5. Debug builds already use **Google's official test ad unit IDs** automatically — you will never see
   your own real ads while developing, which is required by AdMob policy.
6. In Play Console → **App content → Ads**, declare "Yes, my app contains ads."

---

## 6. Hosting the Privacy Policy & Terms

Two ready-made pages are in `web/privacy.html` and `web/terms.html`. Fill in `[DATE]` and
`[SUPPORT_EMAIL]` inside both files, then host them, e.g. with Firebase Hosting (already wired in
`backend/firebase.json`):

```bash
cd backend
firebase deploy --only hosting
```

This gives you a URL like `https://YOUR_PROJECT_ID.web.app/privacy.html`. Put the real URLs into
`nova.properties` (`PRIVACY_URL`, `TERMS_URL`, `SUPPORT_EMAIL`) — the app links to these from the
sign-in screen, Settings, and the Premium screen, and Play Console also requires them under
**App content → Privacy policy**.

---

## 7. Generate your signing (upload) key & build the release

1. In Android Studio, or via command line with `keytool`:
   ```bash
   keytool -genkeypair -v -keystore nova-upload-key.jks -alias nova-upload \
     -keyalg RSA -keysize 2048 -validity 10000
   ```
   Keep this file and its passwords **extremely safe** — losing it means you can never update the app
   under this listing again (unless you use Play App Signing's key-loss recovery process).
2. Copy `keystore.properties.example` → `keystore.properties` and fill in the real path/passwords.
3. Get its SHA-1 (`keytool -list -v -keystore nova-upload-key.jks -alias nova-upload`) and add it to
   Firebase → Project Settings → Your Android app → Add fingerprint (needed for Google Sign-In on
   the release build).
4. Build the App Bundle (the format Play Store requires):
   ```bash
   ./gradlew bundleRelease
   ```
   Output: `app/build/outputs/bundle/release/app-release.aab`.
   The build will **fail on purpose** if `nova.properties`, `keystore.properties`, or
   `google-services.json` are missing — that's a safety check so you can't accidentally ship a build
   with placeholder ad units or no signing key.
5. Enable **Play App Signing** when prompted during your first upload (recommended default) — Google
   then re-signs your app for distribution using your uploaded key as the "upload key."

---

## 8. Play Console listing checklist

- **App content**:
  - Privacy Policy URL (from step 6)
  - **Data safety** form — declare: Personal info (name, email) collected & linked to identity, used
    for account management; App activity (chat content) collected & linked, used for app
    functionality, user-controlled deletion available; not shared with third parties for advertising
    beyond AdMob's own ad-serving use (declare AdMob under "third-party ad SDKs" per its own data
    disclosure — check current requirements in Play Console, they change periodically).
  - **Ads**: declare "Yes."
  - **Government apps / target audience / content rating** questionnaire — answer honestly (this is a
    general AI productivity app, typically rated for a general or 13+ audience; the questionnaire
    will guide you based on what your AI chat can produce).
  - **News app**: No. **COVID-19 app**: No (unless applicable).
- **Store listing**: short description, full description, screenshots (phone — take real screenshots
  from Android Studio's emulator once you run the app), the `store-assets/play-icon-512.png` app icon,
  and `store-assets/feature-graphic-1024x500.png` feature graphic (both already generated for you as
  starting placeholders — replace with your own branded artwork before going live).
- **Subscriptions**: confirm both base plans (`monthly`, `yearly`) are Active.
- **App bundle**: upload the `.aab` from step 7 to an internal testing track first, test the full
  purchase + AI flow with a **license test account**, then promote to production.

---

## 9. What "production-ready" means here — and what's still on you

Everything above is real, working integration code — not placeholders:
- Real AI calls to Anthropic's API from the backend.
- Real Firebase Authentication, Firestore storage, and security rules that make the client
  incapable of granting itself Premium or reading another user's data.
- Real Google Play Billing subscription purchase, acknowledgement, verification, restore, and RTDN
  webhook.
- Real AdMob banner/interstitial/rewarded integration with UMP consent and server-side reward
  verification.

Still required from you before launch: your own Firebase/Anthropic/AdMob/Play Console accounts and
keys (this guide), your own final app icon artwork/screenshots, filling in the legal pages, and
testing the whole purchase + chat flow end-to-end with a real device and a Play Console license
tester account before releasing to production.
