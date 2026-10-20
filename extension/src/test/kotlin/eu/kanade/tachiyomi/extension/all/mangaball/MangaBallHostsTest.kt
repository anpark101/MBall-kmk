package eu.kanade.tachiyomi.extension.all.mangaball

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the Cloudflare host failover: the rules that decide when a request
 * moves to the backup address, and what the reader sees when every address is
 * blocked. Kept free of Android and OkHttp types so it runs as a plain unit test.
 */
class MangaBallHostsTest {

    @Test
    fun `the main address is tried first and the api address backs it up`() {
        assertEquals("mangaball.com", API_HOSTS.first())
        assertTrue("api.mangaball.com" in API_HOSTS)
        assertEquals(API_HOSTS.size, API_HOSTS.distinct().size)
    }

    @Test
    fun `an address that answered is tried first next time`() {
        assertEquals(API_HOSTS, orderHosts(null))
        assertEquals(
            listOf("api.mangaball.com", "mangaball.com"),
            orderHosts("api.mangaball.com"),
        )
        // A host outside the list is ignored rather than trusted.
        assertEquals(API_HOSTS, orderHosts("example.com"))
        assertEquals(API_HOSTS, orderHosts(""))
    }

    @Test
    fun `the reader app's bypass failure is recognized`() {
        assertTrue(IOException("Failed to bypass Cloudflare").isCloudflareBypassFailure())
        // The app's message is translated on the phone, so the class name of the
        // original exception is matched as well.
        assertTrue(
            IOException("Gagal mengatasi Cloudflare", CloudflareBypassException())
                .isCloudflareBypassFailure(),
        )
    }

    @Test
    fun `our own cloudflare mentions are not bypass failures`() {
        // A 522 says "Cloudflare" in its message but is the site's origin
        // hiccuping - it must be retried on the same host, not failed over.
        assertFalse(HttpStatusException(522).isCloudflareBypassFailure())
        assertFalse(IOException("timeout").isCloudflareBypassFailure())
        assertFalse(IOException("Unexpected end of input").isCloudflareBypassFailure())
    }

    @Test
    fun `challenge statuses are the cloudflare ones`() {
        assertTrue(isChallengeStatus(403))
        assertTrue(isChallengeStatus(503))
        assertFalse(isChallengeStatus(200))
        assertFalse(isChallengeStatus(404))
        assertFalse(isChallengeStatus(429))
        assertFalse(isChallengeStatus(522))
    }

    @Test
    fun `an html answer is recognized as a challenge page`() {
        assertTrue(looksLikeHtml("<!DOCTYPE html><html><body>Just a moment...</body></html>"))
        assertTrue(looksLikeHtml("   \n<html lang=\"en\">"))
        assertFalse(looksLikeHtml("{\"code\":200,\"status\":\"success\"}"))
        assertFalse(looksLikeHtml(""))
    }

    @Test
    fun `the block message names every tried address and the fixes`() {
        val message = cloudflareBlockedMessage(API_HOSTS)
        assertTrue(message, message.contains("mangaball.com"))
        assertTrue(message, message.contains("api.mangaball.com"))
        assertTrue(message, message.contains("browser"))
        assertTrue(message, message.contains("Wi-Fi"))
        assertTrue(message, message.contains("DNS over HTTPS"))
        assertTrue(message, message.contains("VPN"))
    }
}

/** Named exactly like the reader app's private exception, so the cause-chain match works. */
private class CloudflareBypassException : Exception()
