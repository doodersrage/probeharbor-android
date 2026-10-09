# Play release checklist

Do this after Play Console developer verification completes.

## One-time setup

1. **Backup upload key** (already generated locally, gitignored):
   - `upload-keystore.jks`
   - `keystore.properties`
   - Store both in a password manager / encrypted backup. Losing them blocks updates unless you reset the upload key with Google support.
2. Create the app in Play Console with application id **`dev.probeharbor.android`**.
3. Enroll in **Play App Signing** when prompted (first AAB upload).
4. Paste listing copy from [`STORE_LISTING.md`](STORE_LISTING.md).
5. Upload graphics from [`assets/`](assets/).
6. Fill Data safety from [`DATA_SAFETY.md`](DATA_SAFETY.md).
7. Set privacy policy URL: https://probeharbor.dev/privacy
8. Complete content rating, target audience, ads (none), news (no).
9. Add phone screenshots under `play/screenshots/` then upload to Console.

## Build a release App Bundle

From the repo root (JDK 17, `local.properties` + `app/google-services.json` present):

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
./gradlew :app:bundleRelease
```

Output:

`app/build/outputs/bundle/release/app-release.aab`

Upload that AAB to an **Internal testing** track first.

## Before each new store version

1. Bump `versionCode` (integer, always +1) and `versionName` in `app/build.gradle.kts`.
2. `./gradlew :app:bundleRelease`
3. Upload AAB + write release notes.

## Demo account for reviewers (recommended)

Create a ProbeHarbor user with sample readings and put credentials in **App content → App access**.
