package com.example.widgetfatsecret.fatsecret.oauth

import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Pure, dependency-free OAuth 1.0a signing (HMAC-SHA1).
 *
 * This class has no Android dependencies on purpose so it can be exercised by
 * plain JVM unit tests using the well-known reference vectors from the OAuth
 * spec / RFC 5849 (see [OAuth1SignerTest]).
 *
 * Nothing here logs secrets, tokens or the produced signature.
 */
object OAuth1Signer {

    /**
     * RFC 3986 percent-encoding. Only the unreserved characters
     * `A-Z a-z 0-9 - . _ ~` are left untouched; everything else (including the
     * space, which must NOT become `+`) is encoded as upper-case `%XX`.
     */
    fun percentEncode(value: String): String {
        val bytes = value.toByteArray(Charsets.UTF_8)
        val sb = StringBuilder(bytes.size * 3)
        for (b in bytes) {
            val c = b.toInt() and 0xFF
            if (c in 'A'.code..'Z'.code ||
                c in 'a'.code..'z'.code ||
                c in '0'.code..'9'.code ||
                c == '-'.code || c == '.'.code || c == '_'.code || c == '~'.code
            ) {
                sb.append(c.toChar())
            } else {
                sb.append('%')
                sb.append(HEX[c shr 4])
                sb.append(HEX[c and 0x0F])
            }
        }
        return sb.toString()
    }

    /**
     * Builds the normalized parameter string: every key and value is
     * percent-encoded, then pairs are sorted by encoded key (ties broken by
     * encoded value) and joined with `&` as `key=value`.
     */
    fun normalizeParameters(params: Map<String, String>): String {
        return params
            .map { (k, v) -> percentEncode(k) to percentEncode(v) }
            .sortedWith(compareBy({ it.first }, { it.second }))
            .joinToString("&") { (k, v) -> "$k=$v" }
    }

    /**
     * Builds the signature base string:
     * `HTTP-METHOD & percentEncode(baseUrl) & percentEncode(normalizedParams)`.
     *
     * [baseUrl] must be the URL without any query string and without a trailing
     * `?` (scheme + authority + path). [params] must contain every request
     * parameter EXCEPT `oauth_signature` (i.e. query params, form-body params
     * and every `oauth_*` protocol parameter).
     */
    fun signatureBaseString(
        method: String,
        baseUrl: String,
        params: Map<String, String>,
    ): String {
        val normalized = normalizeParameters(params)
        return buildString {
            append(method.uppercase())
            append('&')
            append(percentEncode(baseUrl))
            append('&')
            append(percentEncode(normalized))
        }
    }

    /** HMAC-SHA1 signing key: `percentEncode(consumerSecret)&percentEncode(tokenSecret)`. */
    fun signingKey(consumerSecret: String, tokenSecret: String?): String {
        return percentEncode(consumerSecret) + "&" + percentEncode(tokenSecret ?: "")
    }

    /** Returns the Base64 HMAC-SHA1 of [baseString] using [key]. */
    fun hmacSha1(baseString: String, key: String): String {
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA1"))
        val raw = mac.doFinal(baseString.toByteArray(Charsets.UTF_8))
        return base64(raw)
    }

    /**
     * High-level entry point. Produces the full set of oauth_* parameters
     * (including the computed `oauth_signature`) for a request.
     *
     * @param method       HTTP method (GET/POST).
     * @param baseUrl       request URL without query string.
     * @param requestParams non-oauth parameters (query and/or form body).
     * @param consumerKey    consumer key.
     * @param consumerSecret consumer secret.
     * @param token          oauth_token, or null for the request_token step.
     * @param tokenSecret    oauth_token_secret, or null.
     * @param extraOAuth     extra protocol params, e.g. oauth_callback / oauth_verifier.
     * @param nonce          override for deterministic tests; random otherwise.
     * @param timestamp      override for deterministic tests; now otherwise.
     */
    fun buildOAuthParams(
        method: String,
        baseUrl: String,
        requestParams: Map<String, String>,
        consumerKey: String,
        consumerSecret: String,
        token: String? = null,
        tokenSecret: String? = null,
        extraOAuth: Map<String, String> = emptyMap(),
        nonce: String = newNonce(),
        timestamp: Long = System.currentTimeMillis() / 1000L,
    ): Map<String, String> {
        val oauth = LinkedHashMap<String, String>()
        oauth["oauth_consumer_key"] = consumerKey
        oauth["oauth_nonce"] = nonce
        oauth["oauth_signature_method"] = "HMAC-SHA1"
        oauth["oauth_timestamp"] = timestamp.toString()
        oauth["oauth_version"] = "1.0"
        if (!token.isNullOrEmpty()) oauth["oauth_token"] = token
        oauth.putAll(extraOAuth)

        val allParams = HashMap<String, String>()
        allParams.putAll(requestParams)
        allParams.putAll(oauth)

        val baseString = signatureBaseString(method, baseUrl, allParams)
        val key = signingKey(consumerSecret, tokenSecret)
        oauth["oauth_signature"] = hmacSha1(baseString, key)
        return oauth
    }

    /** Builds an `Authorization: OAuth ...` header value from oauth_* params. */
    fun authorizationHeader(oauthParams: Map<String, String>, realm: String? = null): String {
        val sb = StringBuilder("OAuth ")
        if (realm != null) {
            sb.append("realm=\"").append(percentEncode(realm)).append("\", ")
        }
        sb.append(
            oauthParams
                .filterKeys { it.startsWith("oauth_") }
                .toSortedMap()
                .entries
                .joinToString(", ") { (k, v) ->
                    "${percentEncode(k)}=\"${percentEncode(v)}\""
                }
        )
        return sb.toString()
    }

    fun newNonce(): String {
        val bytes = ByteArray(16)
        SECURE_RANDOM.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private val SECURE_RANDOM = SecureRandom()
    private val HEX = "0123456789ABCDEF".toCharArray()

    // Minimal Base64 encoder (JVM's java.util.Base64 is available on Android 26+,
    // but this keeps the class free of android.* and fully unit-testable).
    private const val B64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    private fun base64(data: ByteArray): String {
        val sb = StringBuilder(((data.size + 2) / 3) * 4)
        var i = 0
        while (i < data.size) {
            val b0 = data[i].toInt() and 0xFF
            val b1 = if (i + 1 < data.size) data[i + 1].toInt() and 0xFF else 0
            val b2 = if (i + 2 < data.size) data[i + 2].toInt() and 0xFF else 0
            sb.append(B64[b0 shr 2])
            sb.append(B64[((b0 and 0x03) shl 4) or (b1 shr 4)])
            sb.append(if (i + 1 < data.size) B64[((b1 and 0x0F) shl 2) or (b2 shr 6)] else '=')
            sb.append(if (i + 2 < data.size) B64[b2 and 0x3F] else '=')
            i += 3
        }
        return sb.toString()
    }
}
