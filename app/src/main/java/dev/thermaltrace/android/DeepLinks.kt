package dev.thermaltrace.android

/** Intent extras and destinations for notification / deep-link navigation. */
object DeepLinks {
    const val EXTRA_DESTINATION = "deep_link_destination"
    const val EXTRA_EVENT_ID = "event_id"
    const val EXTRA_OAUTH_EXCHANGE = "oauth_exchange"

    const val ALERTS = "alerts"
    const val HOME = "home"
    const val HISTORY = "history"
    const val PORTFOLIO = "portfolio"
    const val MFA = "mfa"

    /** Matches applicationId; the server routes the sign-in return by it. */
    const val OAUTH_SCHEME = "dev.probeharbor.android"
    const val OAUTH_HOST = "oauth"
    const val OAUTH_HTTPS_HOST = "probeharbor.dev"
    const val OAUTH_HTTPS_PATH = "/app/oauth"
    const val OAUTH_EXCHANGE_PARAM = "exchange"
}
