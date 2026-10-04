# H2 Hunger Hunter — Food Cart Manager

Android app (Kotlin, Jetpack Compose, Material 3, MVVM + Clean Architecture, Hilt) for the daily revenue,
expenses and profit/loss of several food carts. Backend: Firebase Authentication + Cloud Firestore only
(no Firebase Storage, no images).

- **Admin** sees every cart, manager and record; manages carts and manager assignments; reports and charts.
- **Manager** sees and adds data only for the one cart an admin assigned to them.
- Everyone who registers is a **manager** with no cart. Admins are made only in the Firebase console.
- Access is enforced by `firestore.rules`, not just the UI (32 emulator tests in `firestore-tests/`).

## Firebase setup (one time)

1. Firebase console → **Add project** (Google Analytics not needed).
2. **Build → Authentication → Get started → Sign-in method → Email/Password → Enable**.
3. **Build → Firestore Database → Create database**: *Standard edition*, ID `(default)`,
   location `asia-south1 (Mumbai)`, **Start in production mode**.
4. **Project settings → Your apps → Add app → Android**, package name `com.neoqubix.devajit.h2`.
   Download `google-services.json` into `app/`. (The app does not build without it.)
5. Deploy the rules and indexes from this folder:
   ```
   firebase login
   firebase use --add            # pick the new project
   firebase deploy --only firestore
   ```
   Or paste `firestore.rules` into Firestore → Rules → Publish, and create the two composite indexes from
   `firestore.indexes.json` (`sales` and `expenses`: `cartId` ascending, `date` descending).
6. Create the first admin: register in the app, then in Firestore → `users/{your uid}` change `role` from
   `manager` to `admin`. The app switches to the admin dashboard immediately.

## Firestore data

| Collection | Fields |
|---|---|
| `users/{uid}` | name, email, phone, role (`manager`/`admin`), cartId (null until assigned), createdAt, updatedAt |
| `carts/{cartId}` | name, location, managerId, status (`active`/`inactive`), createdAt, updatedAt |
| `sales/{id}` | cartId, amount, date, description, createdBy, createdAt, updatedAt |
| `expenses/{id}` | cartId, category, amount, date, description, createdBy, createdAt, updatedAt |

Profit/loss is never stored: it is always revenue − expenses calculated from these records.

## Project layout

```
app/src/main/java/com/neoqubix/devajit/h2/
├── data        firebase (schema, mappers, flows) + repository implementations
├── domain      models, repository interfaces, use cases, ReportCalculator
├── presentation auth · manager · admin (dashboard, carts, managers, reports) · components · navigation
├── di          Hilt modules
└── utils       ₹ formatting, dates, error messages
```

## Checks

```
./gradlew :app:testDebugUnitTest                 # money/date calculations
cd firestore-tests && npm install && npm test    # security rules on the emulator (needs Java 21+)
```

## Releases and in-app updates (no Play Store)

Releases are built by **Actions → Release APK → Run workflow** (`.github/workflows/release.yml`):

- **bump**: patch / minor / major from the last `vX.Y.Z` tag (the first release is `1.0.0`).
  `versionCode = MAJOR*10000 + MINOR*100 + PATCH`.
- **release_notes**: shown in the app's update dialog.
- **force_update**: users on older versions must update before they can use the app.

The workflow builds and signs the APK, verifies the signature, and publishes a GitHub Release with
`h2-vX.Y.Z.apk` and `update.json` (version, minimum supported version, APK link, SHA-256, notes).

The app reads `https://github.com/DevP-ai/H2/releases/latest/download/update.json` on every launch
(release builds only) and from **Profile → Check for updates**. If a newer version exists it shows
**Update available** (Later / Update now) or **Update required** (can't be dismissed), downloads the APK with a
progress bar, checks its SHA-256, and opens the Android installer. The first time, Android asks to allow
"Install unknown apps" for H2.

First install for a new user: send them the APK link from the latest release page.

### Signing secrets

Add these in **Settings → Secrets and variables → Actions**: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD`. Every release must use the same keystore: if it is lost, installed apps can
never be updated (users would have to uninstall and reinstall). Keep a backup outside this repo.
