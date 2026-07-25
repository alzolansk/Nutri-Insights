package com.example.widgetfatsecret.fatsecret.data

/** High-level classification of a sync failure, used to drive UI/widget state. */
enum class SyncErrorType {
    NO_CREDENTIALS,   // consumer key/secret not filled in local.properties
    NOT_CONNECTED,    // no access token stored yet
    NETWORK,          // no internet / timeout
    RATE_LIMIT,       // FatSecret throttling
    SERVER,           // 5xx or transient server error
    AUTH_INVALID,     // token/verifier/signature rejected
    EMPTY,            // empty/unparseable response body
    UNKNOWN,
}

/** Thrown when FatSecret returns a JSON `error` object. */
class FatSecretApiException(
    val code: Int,
    val apiMessage: String,
) : Exception("FatSecret API error $code") {

    fun toSyncErrorType(): SyncErrorType = when (code) {
        // See https://platform.fatsecret.com/docs/guides/authentication/error-codes
        2, 3, 4, 5, 6, 7 -> SyncErrorType.AUTH_INVALID   // missing/invalid oauth params, signature
        8, 9 -> SyncErrorType.AUTH_INVALID               // invalid/expired token
        12 -> SyncErrorType.AUTH_INVALID                 // user not authorized
        21 -> SyncErrorType.RATE_LIMIT                   // too many requests / IP not whitelisted
        else -> SyncErrorType.SERVER
    }
}

/** Thrown when required consumer credentials are missing. */
class MissingCredentialsException : Exception("FatSecret consumer key/secret not configured")

/** Thrown when the app has no stored access token. */
class NotConnectedException : Exception("Not connected to FatSecret")
