package eu.kanade.tachiyomi.extension.all.mangaball

import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the typo-tolerant search scoring. The junk pool in
 * [the reported junk list keeps only the searched title] is the exact list the user saw when
 * searching `mieruko-ct`: the site's fuzzy search had ranked unrelated `-chan` titles above the
 * title that was asked for, because the search was combined with `sort_by=lastupdate`.
 */
class MangaBallSearchTest {

    private fun manga(title: String, alternativeTitles: List<String> = emptyList()): SManga =
        SManga.create().apply {
            url = title.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
            this.title = title
            // SManga.memo is a JsonObject in the extension API - build it directly.
            memo = buildJsonObject {
                put(
                    "altTitles",
                    buildJsonArray { alternativeTitles.forEach { add(JsonPrimitive(it)) } },
                )
            }
        }

    private fun score(query: String, title: String): Double = SmartSearch.similarity(query, title)

    // ------------------------------------------------------------------ normalization

    @Test
    fun `normalization ignores case, accents, punctuation and repeated spaces`() {
        assertEquals("mieruko chan", SmartSearch.normalize("  Mieruko-CHAN! "))
        assertEquals("cafe creme", SmartSearch.normalize("Café  Crème"))
        assertEquals("one piece", SmartSearch.normalize("One_Piece"))
        assertEquals("", SmartSearch.normalize("   ---   "))
    }

    @Test
    fun `normalization keeps non latin scripts intact`() {
        assertEquals("見える子ちゃん", SmartSearch.normalize("見える子ちゃん"))
        assertEquals("naruto ナルト", SmartSearch.normalize("Naruto・ナルト"))
    }

    // ------------------------------------------------------------------ similarity

    @Test
    fun `exact and substring titles score highest`() {
        assertEquals(1.0, score("naruto", "Naruto"), 0.001)
        assertTrue(score("mieruko", "Mieruko-chan") >= 0.95)
    }

    @Test
    fun `a missing space or hyphen does not matter`() {
        assertEquals(1.0, score("sololeveling", "Solo Leveling"), 0.001)
        assertEquals(1.0, score("mierukochan", "Mieruko-chan"), 0.001)
    }

    @Test
    fun `single typos keep a high score`() {
        assertTrue("transposed letters", score("nartuo", "Naruto") >= 0.8)
        assertTrue("transposed letters", score("peice", "Piece") >= 0.75)
        assertTrue("wrong letter", score("mieruko-chanm", "Mieruko-chan") >= 0.8)
    }

    @Test
    fun `a half typed second word still matches its title`() {
        // The exact query from the user's screenshot: "mieruko-ct" is "Mieruko-chan" with the
        // last word barely started.
        val matched = score("mieruko-ct", "Mieruko-chan")
        assertTrue("expected >= 0.6 but was $matched", matched >= 0.6)
        assertTrue(matched >= SmartSearch.KEEP)
    }

    @Test
    fun `multi word queries need most of their words`() {
        assertTrue(score("one peice", "One Piece") >= 0.8)
        assertTrue(score("attack on titan", "Shingeki no Kyojin") < SmartSearch.WEAK)
    }

    @Test
    fun `unrelated titles score below the junk threshold`() {
        listOf(
            "Super! Makoto-Chan",
            "Onii-chan Is Done For!",
            "Ri-chan",
            "Toudou-san to Nia-chan",
            "You are Bun-chan's Love",
            "Amaechau?",
        ).forEach { title ->
            val junk = score("mieruko-ct", title)
            assertTrue("$title scored $junk", junk < SmartSearch.WEAK)
        }
    }

    @Test
    fun `alternative titles are searched too`() {
        val mieruko = manga("見える子ちゃん", listOf("Mieruko-chan", "The Girl Who Can See Them"))
        assertTrue(scoreManga("mieruko", mieruko) >= 0.95)
        assertTrue(scoreManga("the girl who can see them", mieruko) >= 0.95)
        assertTrue(scoreManga("見える子ちゃん", mieruko) >= 0.99)
    }

    // ------------------------------------------------------------------ variants

    @Test
    fun `variant list offers the distinctive word of the query`() {
        // "mieruko-ct" trails off; "mieruko" alone is what makes the server find Mieruko-chan.
        val variants = SmartSearch.candidateQueries("mieruko-ct")
        assertTrue("expected 'mieruko' in $variants", "mieruko" in variants)
    }

    @Test
    fun `variant list corrects a typo inside the phrase`() {
        // "one peice" must be retried as "one piece", not as the squashed typo "onepeice".
        val variants = SmartSearch.candidateQueries("one peice")
        assertTrue("expected 'one piece' in $variants", "one piece" in variants)
    }

    @Test
    fun `variant list removes spaces the user typed`() {
        assertTrue("sololeveling" in SmartSearch.candidateQueries("solo leveling"))
    }

    @Test
    fun `variant list fixes a transposed typo`() {
        assertTrue("naruto" in SmartSearch.candidateQueries("nartuo"))
        assertTrue("piece" in SmartSearch.candidateQueries("peice"))
    }

    @Test
    fun `variant list never repeats the original query`() {
        val original = SmartSearch.normalize("Mieruko-ct")
        SmartSearch.candidateQueries("Mieruko-ct").forEach { variant ->
            assertTrue("$variant repeats the query", SmartSearch.normalize(variant) != original)
        }
    }

    @Test
    fun `variant list stays short and avoids blank entries`() {
        val variants = SmartSearch.candidateQueries("mieruko-ct")
        assertTrue(variants.size <= 12)
        assertTrue(variants.none { it.isBlank() })
        assertTrue(SmartSearch.candidateQueries("").isEmpty())
    }

    // ------------------------------------------------------------------ ranking

    @Test
    fun `the reported junk list keeps only the searched title`() {
        val pool = listOf(
            manga("Super! Makoto-Chan"),
            manga("You are Bun-chan's Love"),
            manga("Onii-chan Is Done For!"),
            manga("Ri-chan"),
            manga("Mieruko-chan", listOf("見える子ちゃん", "Mieruko-Chan")),
            manga("Toudou-san to Nia-chan"),
        )
        val ranked = SmartSearch.rankForQuery("mieruko-ct", pool)
        assertEquals(1, ranked.size)
        assertEquals("Mieruko-chan", ranked.first().title)
    }

    @Test
    fun `relevant results come first and junk is dropped`() {
        val pool = listOf(
            manga("Boruto"),
            manga("One Piece"),
            manga("Naruto"),
            manga("Naruto Gaiden"),
        )
        val ranked = SmartSearch.rankForQuery("naruto", pool)
        assertEquals("Naruto", ranked.first().title)
        assertTrue(ranked.none { it.title == "One Piece" })
        assertTrue(ranked.size >= 2)
    }

    @Test
    fun `equal scores keep the order the server returned`() {
        val pool = listOf(manga("Naruto"), manga("Naruto Gaiden"), manga("Naruto Shippuden"))
        val ranked = SmartSearch.rankForQuery("naruto", pool)
        assertEquals(listOf("Naruto", "Naruto Gaiden", "Naruto Shippuden"), ranked.map { it.title })
    }

    @Test
    fun `when nothing matches the servers answer is trusted`() {
        // A search for an author, a description word or a scanlation group can legitimately
        // return titles whose *names* have nothing to do with the query - dropping those would
        // break the search, so the server's answer must survive untouched.
        val pool = listOf(manga("Naruto"), manga("One Piece"), manga("Bleach"))
        assertEquals(pool, SmartSearch.rankForQuery("izumi tomoki", pool))
    }

    @Test
    fun `empty result sets are returned as is`() {
        assertEquals(emptyList<SManga>(), SmartSearch.rankForQuery("naruto", emptyList()))
        val single = listOf(manga("Naruto"))
        assertEquals(single, SmartSearch.rankForQuery("naruto", single))
    }

    // ------------------------------------------------------------------ distance

    @Test
    fun `edit distance counts a transposition as one edit`() {
        assertEquals(0, SmartSearch.editDistance("naruto", "naruto", 6))
        assertEquals(1, SmartSearch.editDistance("nartuo", "naruto", 6))
        assertEquals(1, SmartSearch.editDistance("peice", "piece", 6))
        assertEquals(2, SmartSearch.editDistance("", "ab", 6))
        // Distance work is abandoned as soon as it cannot stay under the cap.
        assertEquals(7, SmartSearch.editDistance("a", "abcdefghijklmnop", 6))
    }
}
