# Play Console — store listing copy

Paste these into Play Console when verification finishes. Package name must be:

`dev.probeharbor.android`

Privacy policy URL: https://probeharbor.dev/privacy  
Terms: https://probeharbor.dev/terms  
Support / contact: https://probeharbor.dev/contact  
App website: https://probeharbor.dev

## App name (30 chars max)

ProbeHarbor

## Short description (80 chars max)

Monitor probe temperatures and freeze alerts from your ProbeHarbor account.

## Full description

ProbeHarbor is the Android companion for probeharbor.dev. Sign in with your existing account to view live probe readings, history, alerts, devices, and household settings — the same account data as the web app.

The phone does not collect temperature itself. Sensors stay on your ESP/home hardware; this app talks to ProbeHarbor over HTTPS.

Features:
• Live home readings with space filters and auto-refresh
• Heating and condensation insights on Home
• History sparklines (24h / 7d / 30d)
• Alert inbox, acknowledge, and threshold settings
• Device rename and space assignment
• Household members and invites
• Optional push notifications (Pro) via Firebase Cloud Messaging

For the full Overview Status strip (freeze hours, probe spread, indoor−outdoor ΔT) and Insights cards (doors, power, air quality, RSSI), open the web Overview at https://probeharbor.dev/dashboard — live probe cards on the web are at https://probeharbor.dev/dashboard/live. Charts on Overview/History can overlay humidity and dew point.

Account required. Create or manage your account at https://probeharbor.dev

Delete your account anytime: sign in on the web → Dashboard → Settings → Delete account.

## Category

Tools (or Productivity / House & Home if preferred)

## Tags / keywords (internal notes only)

temperature, garage, workshop, attic, freeze, humidity, IoT, ESP32, alerts, monitoring

## Contact

Email: use the same address you verify in Play Console  
Website: https://probeharbor.dev  
Privacy: https://probeharbor.dev/privacy

## Graphics (in play/assets/)

| Asset | File | Size |
|-------|------|------|
| High-res icon | `icon-512.png` | 512×512 |
| Feature graphic | `feature-graphic-1024x500.png` | 1024×500 |
| Optional promo | `promo-1280x720.png` | 1280×720 |

## Screenshots (you still need to capture)

Put phone screenshots in `play/screenshots/` (gitignored). Play requires at least 2 phone screenshots.

Suggested set (1080×1920 or similar):
1. Login
2. Home live readings
3. History
4. Alerts inbox
5. Devices or Account (push toggle)

Capture from a real device or emulator after signing in.

## Content rating

IARC questionnaire — answer for a utility app:
- No violence, sexual content, or gambling
- No user-generated public content feed
- Account login / online features: yes
- Shares location: no
- Users can communicate: household invites only (private, not a public social network)

Expected rating: Everyone / PEGI 3 style (confirm after questionnaire).

## Target audience

Age 18+ recommended (home monitoring / billing account). Not designed for children. Do **not** check “Designed for children.”

## News / COVID / finance

None of the special categories apply.

## Ads

No ads.

## In-app purchases / subscriptions

None in the Android app. Paid plans (Member/Pro) are managed on the website via Stripe. In Play Console: declare that the app does not sell digital goods through Google Play (external account management is fine for this utility).

## App access

If reviewers need a login, create a dedicated demo account and add it under **App content → App access**.
