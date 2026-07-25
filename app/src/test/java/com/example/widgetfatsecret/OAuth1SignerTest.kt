package com.example.widgetfatsecret

import com.example.widgetfatsecret.fatsecret.oauth.OAuth1Signer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OAuth1SignerTest {

    // --- percent-encoding (RFC 3986) -----------------------------------------

    @Test
    fun unreservedCharactersAreNotEncoded() {
        val unreserved = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~"
        assertEquals(unreserved, OAuth1Signer.percentEncode(unreserved))
    }

    @Test
    fun spaceEncodesAsPercent20NotPlus() {
        assertEquals("%20", OAuth1Signer.percentEncode(" "))
        assertEquals("Ladies%20%2B%20Gentlemen", OAuth1Signer.percentEncode("Ladies + Gentlemen"))
    }

    @Test
    fun reservedCharactersAreUpperCaseHex() {
        assertEquals("%3D", OAuth1Signer.percentEncode("="))
        assertEquals("%26", OAuth1Signer.percentEncode("&"))
        assertEquals("%2F", OAuth1Signer.percentEncode("/"))
    }

    @Test
    fun encodesUtf8MultibyteCharacters() {
        // "é" -> UTF-8 0xC3 0xA9
        assertEquals("%C3%A9", OAuth1Signer.percentEncode("é"))
    }

    // --- normalization / ordering --------------------------------------------

    @Test
    fun parametersAreSortedByEncodedKeyAndEncodedValue() {
        val params = mapOf("z" to "1", "a" to "two words", "m" to "a+b")
        assertEquals(
            "a=two%20words&m=a%2Bb&z=1",
            OAuth1Signer.normalizeParameters(params),
        )
    }

    // --- signature base string -----------------------------------------------

    @Test
    fun buildsExpectedSignatureBaseString() {
        val params = mapOf("z" to "1", "a" to "two words", "m" to "a+b")
        val base = OAuth1Signer.signatureBaseString("get", "http://example.com/path", params)
        assertEquals(
            "GET&http%3A%2F%2Fexample.com%2Fpath&a%3Dtwo%2520words%26m%3Da%252Bb%26z%3D1",
            base,
        )
    }

    // --- HMAC-SHA1 (known vector) ---------------------------------------------

    @Test
    fun hmacSha1MatchesKnownVector() {
        // RFC-style reference vector: HMAC-SHA1("The quick brown fox...", key="key")
        val signature = OAuth1Signer.hmacSha1(
            baseString = "The quick brown fox jumps over the lazy dog",
            key = "key",
        )
        assertEquals("3nybhbi3iqa8ino29wqQcBydtNk=", signature)
    }

    @Test
    fun signingKeyConcatenatesEncodedSecrets() {
        assertEquals("con%26sumer&tok%26en", OAuth1Signer.signingKey("con&sumer", "tok&en"))
        assertEquals("secret&", OAuth1Signer.signingKey("secret", null))
    }

    // --- full oauth params ----------------------------------------------------

    @Test
    fun buildOAuthParamsIsDeterministicAndComplete() {
        val a = OAuth1Signer.buildOAuthParams(
            method = "GET",
            baseUrl = "https://platform.fatsecret.com/rest/server.api",
            requestParams = mapOf("method" to "food_entries.get.v2", "format" to "json", "date" to "20000"),
            consumerKey = "ck",
            consumerSecret = "cs",
            token = "tok",
            tokenSecret = "ts",
            nonce = "fixednonce",
            timestamp = 1_600_000_000L,
        )
        val b = OAuth1Signer.buildOAuthParams(
            method = "GET",
            baseUrl = "https://platform.fatsecret.com/rest/server.api",
            requestParams = mapOf("method" to "food_entries.get.v2", "format" to "json", "date" to "20000"),
            consumerKey = "ck",
            consumerSecret = "cs",
            token = "tok",
            tokenSecret = "ts",
            nonce = "fixednonce",
            timestamp = 1_600_000_000L,
        )
        assertEquals("1.0", a["oauth_version"])
        assertEquals("HMAC-SHA1", a["oauth_signature_method"])
        assertEquals("tok", a["oauth_token"])
        assertTrue(a["oauth_signature"]!!.isNotEmpty())
        // Deterministic for identical inputs.
        assertEquals(a["oauth_signature"], b["oauth_signature"])
    }

    @Test
    fun authorizationHeaderIsSortedAndQuoted() {
        val header = OAuth1Signer.authorizationHeader(
            mapOf(
                "oauth_signature" to "sig+val",
                "oauth_consumer_key" to "ck",
                "not_oauth" to "ignored",
            )
        )
        assertTrue(header.startsWith("OAuth "))
        assertTrue(header.contains("oauth_consumer_key=\"ck\""))
        assertTrue(header.contains("oauth_signature=\"sig%2Bval\""))
        assertTrue(!header.contains("not_oauth"))
        // consumer_key sorts before signature
        assertTrue(header.indexOf("oauth_consumer_key") < header.indexOf("oauth_signature"))
    }
}
