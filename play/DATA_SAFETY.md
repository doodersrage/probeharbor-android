# Play Console — Data safety answers

Use these when filling **App content → Data safety**. Adjust if your product practices change.

Privacy policy: https://probeharbor.dev/privacy

## Overview

| Question | Answer |
|----------|--------|
| Does your app collect or share user data? | **Yes** |
| All user data encrypted in transit? | **Yes** (HTTPS) |
| Do you provide a way for users to request deletion? | **Yes** — web Dashboard → Settings → Delete account; also documented in privacy policy |
| Committed to Play Families Policy? | **No** (not a kids app) |

## Data collected

### Personal info
| Type | Collected | Shared | Purpose | Optional? |
|------|-----------|--------|---------|-----------|
| Email address | Yes | No* | Account management / App functionality | Required to sign in |
| User IDs | Yes (auth subject) | No* | Account management / App functionality | Required |

\*Not sold. Shared only with processors that operate the service (Supabase Auth, hosting). In the form, if they ask “shared with third parties” for advertising: **No**. For service providers acting on your behalf, follow Play’s current wording — typically still declare collection; sharing for “third parties” usually means other developers/advertisers, not infrastructure.

### Location
None collected by the Android app.

### Financial
None collected by the Android app (billing is on the website via Stripe).

### Messages
None (no SMS/in-app chat). Alert *content* is temperature notifications, not user-to-user messages.

### Photos / video / audio / files
None.

### Calendar / contacts
None.

### App activity
| Type | Collected | Shared | Purpose |
|------|-----------|--------|---------|
| App interactions | Minimal (session / API usage on our servers) | No* | App functionality, analytics (server-side product metrics if any — no GA in the Android app) |

### App info and performance
| Type | Collected | Shared | Purpose |
|------|-----------|--------|---------|
| Crash logs | Only if you later enable Play Vitals / Crashlytics | Google (Play) if using Play App Signing / Vitals | Stability |
| Diagnostics | Same | Same | Stability |

Until Crashlytics is added: you can say you do **not** collect crash logs in-app; Play Vitals may still show anonymized crashes to you in Console.

### Device or other IDs
| Type | Collected | Shared | Purpose | Optional? |
|------|-----------|--------|---------|-----------|
| Device or other IDs | Yes — FCM registration token when push is enabled | No* (stored on ProbeHarbor servers to deliver alerts) | App functionality / Developer communications (push alerts) | Optional (user enables push) |

## Data handling specifics

- **Ephemeral processing only?** No — account and readings are stored on ProbeHarbor servers.
- **Data deletion:** Yes — account deletion removes alert settings and sole-owned households (see privacy policy).
- **Sensitive data:** Temperature/humidity readings are associated with the user’s household/devices (home monitoring). Treat as app functionality data, not “health” unless Play’s form forces a health category (prefer **App functionality** / **Account management**).

## Security practices checklist

- [x] Data encrypted in transit (TLS)
- [ ] Data encrypted at rest — declare **Yes** if Supabase/DB encryption at rest is enabled (default on managed Postgres); confirm in Supabase project settings
- [x] Users can request data deletion
- [x] Independent security review — **No** (unless you later get one)

## Permissions justification (App content / Declared permissions)

| Permission | Why |
|------------|-----|
| `INTERNET` | Sign-in and load readings/settings from probeharbor.dev |
| `POST_NOTIFICATIONS` | Optional freeze/alert push notifications (Android 13+) |

No precise location, camera, mic, SMS, or contacts.
