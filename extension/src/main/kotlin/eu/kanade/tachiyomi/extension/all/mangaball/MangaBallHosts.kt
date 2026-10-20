package eu.kanade.tachiyomi.extension.all.mangaball

/**
 * The hosts that serve the MangaBall API, in the order they are tried.
 *
 * The site sits behind Cloudflare, and Cloudflare's rules are applied per host: one phone
 * network can be served a challenge (or a hard block) on `mangaball.com` while
 * `api.mangaball.com` - the very same server and database - answers normally. The main
 * address stays first because it is the one the site itself uses; when a request dies in
 * a Cloudflare challenge it is repeated against the next host before the error reaches
 * the reader.
 */
internal val API_HOSTS: List<String> = listOf(
    "mangaball.com",
    "api.mangaball.com",
)

/**
 * The order to try [API_HOSTS] in: the host that answered most recently goes first, so a
 * network that is only blocked on the main address does not sit through a Cloudflare
 * WebView attempt (up to half a minute) on every single request. An unknown or absent
 * host is ignored.
 */
internal fun orderHosts(lastKnownGood: String?): List<String> {
    if (lastKnownGood == null || lastKnownGood !in API_HOSTS) return API_HOSTS
    return listOf(lastKnownGood) + API_HOSTS.filter { it != lastKnownGood }
}

/**
 * Komikku and Mihon solve Cloudflare challenges inside the app and give up with an
 * `IOException` ("Failed to bypass Cloudflare") when the challenge cannot be solved - a
 * hard block, or a check the in-app WebView cannot pass. Those failures are the
 * extension's cue to try the backup address instead of hammering the blocked one.
 *
 * The exception's class name is matched because the app's message is translated on the
 * reader's phone while the class name is not. Our own [HttpStatusException] messages are
 * excluded on purpose: they mention Cloudflare too ("Cloudflare 522"), but a 522 is the
 * site's origin hiccuping and must be retried on the same host, not failed over.
 */
internal fun isCloudflareBypassFailure(error: Throwable): Boolean {
    if (error is HttpStatusException) return false
    if (generateSequence(error) { it.cause }
        .any { it.javaClass.simpleName == "CloudflareBypassException" }
    ) return true
    val message = error.message.orEmpty()
    return message.contains("bypass", ignoreCase = true) &&
        message.contains("cloudflare", ignoreCase = true)
}

/** HTTP statuses Cloudflare uses for its challenge and block pages. */
internal fun isChallengeStatus(code: Int): Boolean = code == 403 || code == 503

/**
 * True when a body is an HTML page (a challenge interstitial served with a 200) rather
 * than the API's JSON.
 */
internal fun looksLikeHtml(body: String): Boolean = body.trimStart().startsWith("<")

/** What the reader sees when Cloudflare blocked every address the extension knows. */
internal fun cloudflareBlockedMessage(hostsTried: Collection<String>): String {
    val hosts = hostsTried.joinToString(", ")
    return "Cloudflare is blocking this connection from your network (tried: $hosts). " +
        "The site itself is fine - it is this network's address that is being challenged, " +
        "which no extension can solve on its own.\n\n" +
        "What to try, in order:\n" +
        "1. Open https://mangaball.com in your phone's browser and pass any check shown " +
        "there, then pull down to refresh in Komikku.\n" +
        "2. Switch between Wi-Fi and mobile data - a different address usually works at once.\n" +
        "3. In Komikku go to More -> Settings -> Advanced and turn on DNS over HTTPS, " +
        "then restart Komikku.\n" +
        "4. A VPN works too, and most blocks clear on their own within an hour."
}
