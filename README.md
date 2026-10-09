# ProbeHarbor for Android

Native Android client for [probeharbor.dev](https://probeharbor.dev). The phone **does not** collect sensor data — it signs in with your ProbeHarbor account and loads live readings from the existing HTTPS API.

## Stack

- Kotlin, Jetpack Compose, Material 3
- Supabase Auth (same project as the web app)
- OkHttp cookie jar → `sb-access-token` / `sb-refresh-token` for dashboard APIs
- Retrofit + kotlinx.serialization → `GET /api/home/readings`

## Setup

1. Open this folder in **Android Studio** (Ladybug / 2024.2+ recommended).
2. Copy `local.properties.example` → `local.properties` and set:

```properties
sdk.dir=/path/to/Android/Sdk
supabase.url=https://YOUR_PROJECT.supabase.co
supabase.anonKey=YOUR_PUBLISHABLE_OR_ANON_KEY
thermaltrace.baseUrl=https://probeharbor.dev
```

Use the **anon / publishable** key only — never the service role key.

3. **Gradle JDK:** Android Studio’s bundled JBR may be Java 25, which is too new for Gradle 8.11. In **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK**, pick **17** (or `/usr/lib/jvm/java-17-openjdk` on Linux). Then Sync.
4. Run on an emulator or device.

From the CLI (with Android SDK installed and JDK 17):

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk   # Linux; use a JDK 17–23 install
./gradlew :app:assembleDebug
```

## How auth works

1. Email/password via Supabase (`signInWithPassword`).
2. Access + refresh tokens are stored locally and attached as cookies on `probeharbor.dev` requests (same contract as the web dashboard).
3. Home: `GET /api/home/readings?save=0` plus heating/condensation insights and **time-to-freeze** from `GET /api/user/home-insights`
4. History: `GET /api/user/history` and Pro claims pack download via `GET /api/claims/pack`
5. Settings load: `GET /api/user/export` (preferences + alert settings + entitlements)
6. Settings save: form `POST` to `/api/user/preferences`, `/api/user/alert-settings`, `/api/user/alert-snooze` (variable hours/days)
7. Share links: `GET`/`POST` `/api/share/manage` (Pro)

**Web Overview parity:** The phone home screen covers live readings, time-to-freeze, and heating/condensation insights (same APIs as web Live). Full Overview Status metrics (freeze hours, indoor−outdoor ΔT, probe spread, feed health) and Insights cards (door/power/motion, air quality, RSSI, energy, flood/level) live on the [web Overview](https://probeharbor.dev/dashboard) — current probe cards on the web are at [Live](https://probeharbor.dev/dashboard/live). Week charts can overlay humidity and dew point.

## Project layout

```
app/src/main/java/dev/thermaltrace/android/
  data/auth/       Session store + Supabase sign-in
  data/api/        Cookie jar, Retrofit, readings + settings + share + claims
  data/insights/   Heating / condensation insight helpers (parity with web)
  data/model/      JSON models
  ui/login/        Sign-in
  ui/home/         Live readings + insights
  ui/history/      Charts + claims pack
  ui/devices/      Device list, rename, space
  ui/alerts/       Alert thresholds, channels, snooze (4h–48h)
  ui/household/    Members, rename, invite, switch
  ui/share/        Guest/family share links
  ui/account/      Display prefs (°C, theme), push, sign out
```

## Play Store release

See [`play/README.md`](play/README.md) for the full checklist, listing copy, and Data safety answers.

Quick path:

1. Upload key is created once via `./scripts/create-upload-keystore.sh` (already done locally; files are gitignored — **back them up**).
2. Ensure `local.properties` and `app/google-services.json` have production values.
3. `./gradlew :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`
4. Upload AAB + assets from `play/assets/` in Play Console (internal testing first).

Privacy policy URL for Console: https://probeharbor.dev/privacy

## Push notifications (FCM)

Pro accounts can receive the same **Push** channel on browser (VAPID) and this app (FCM).

1. Create a Firebase project and add Android apps for `dev.probeharbor.android` and `dev.probeharbor.android.debug`.
2. Download `google-services.json` into `app/` (gitignored). You can start from `app/google-services.json.example`.
3. Optionally also set `firebase.*` keys in `local.properties` if you are not using the Google Services plugin.
4. On the server (`garage-temp` / probeharbor.dev), set `FCM_SERVICE_ACCOUNT_JSON` (or `FCM_*` split fields), run the `fcm_device_tokens` migration, and `pnpm secrets:push`.
5. Sign in on the app → **Account → Enable push**, and on the website enable **Push (browser + Android)** under Alerts.

## License

MIT (aligned with ProbeHarbor).
