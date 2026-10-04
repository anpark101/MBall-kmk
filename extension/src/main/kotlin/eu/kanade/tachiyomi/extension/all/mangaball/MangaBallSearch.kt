package eu.kanade.tachiyomi.extension.all.mangaball

import eu.kanade.tachiyomi.source.model.SManga

/**
 * Typo-tolerant, relevance-first ranking on top of the site's own search.
 *
 * The site's search engine is fuzzy, but it is generous: a query like `mieruko-ct` comes back
 * with "Mieruko-chan" *and* a pile of unrelated `-chan` titles, and when a search is combined
 * with `sort_by=lastupdate` those unrelated titles can even land above the real match. This
 * object supplies:
 *
 *  * [similarity] - a 0..1 score between a query and a title (or one of its alternative titles)
 *    that tolerates typos, missing spaces, hyphens, accents and word order,
 *  * [candidateQueries] - a short, ordered list of alternative spellings to retry on the server
 *    when nothing in the first answer looks like the query,
 *  * [rankForQuery] - the final filter + ordering of a result set.
 *
 * Everything here is plain Kotlin (no Android APIs beyond [SManga], which is a stub in tests), so
 * the whole scoring model is covered by unit tests.
 */
internal object SmartSearch {

    /** The server's answer clearly contains what was asked for - no retries needed. */
    const val STRONG: Double = 0.80

    /** Results below this score are dropped when a better match exists. */
    const val KEEP: Double = 0.50

    /** Below this nothing looks like the query at all; the server's own answer is trusted. */
    const val WEAK: Double = 0.34

    /** How many extra requests one search may spend on typo variants. */
    const val MAX_PROBES: Int = 3

    /**
     * Lowercases, strips accents and turns every run of punctuation or spaces into a single
     * space, so that "Mieruko-Chan!", "mieruko chan" and "MIERUKO  CHAN" all become
     * "mieruko chan". CJK and other letter-based scripts are preserved as-is.
     */
    fun normalize(text: String): String {
        val decomposed = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
        val builder = StringBuilder(decomposed.length)
        var pendingSpace = false
        for (character in decomposed) {
            when {
                character.code in 0x0300..0x036F -> Unit // combining accent left over from NFD
                character.isLetterOrDigit() -> {
                    if (pendingSpace && builder.isNotEmpty()) builder.append(' ')
                    pendingSpace = false
                    builder.append(character.lowercaseChar())
                }
                else -> pendingSpace = true
            }
        }
        return builder.toString()
    }

    fun tokens(normalized: String): List<String> =
        normalized.split(' ').filter { it.isNotEmpty() }

    /**
     * How close [target] is to [query], 0..1. Tuned for titles:
     *
     *  * exact / substring / prefix matches score ~1.0 ("mieruko" - "Mieruko-chan"),
     *  * one typo in a word keeps a high score ("nartuo" - "Naruto" = 0.83),
     *  * spaces and hyphens do not matter ("sololeveling" - "Solo Leveling" = 1.0),
     *  * multi-word queries require most words to be present, so "mieruko-ct" still matches
     *    "Mieruko-chan" (one word exact, one half-typed) while "Super! Makoto-Chan" scores ~0.1.
     */
    fun similarity(query: String, target: String): Double {
        val q = normalize(query)
        val t = normalize(target)
        if (q.isEmpty() || t.isEmpty()) return 0.0
        if (q == t) return 1.0
        if (t.length > q.length && t.contains(q)) return 0.97

        val qWords = tokens(q)
        val tWords = tokens(t)
        if (qWords.isEmpty() || tWords.isEmpty()) return 0.0

        val packedQuery = q.replace(" ", "")
        val packedTarget = t.replace(" ", "")
        val joined = if (packedQuery.isEmpty() || packedTarget.isEmpty()) {
            0.0
        } else {
            1.0 - editDistance(packedQuery, packedTarget, 12).toDouble() /
                maxOf(packedQuery.length, packedTarget.length)
        }

        val perWord = qWords.map { word -> tWords.maxOf { candidate -> tokenSimilarity(word, candidate) } }
        val coverage = perWord.average()

        return when {
            qWords.size == 1 -> maxOf(joined, coverage)
            // One word matches, the rest is missing: that is somebody else's title.
            coverage < 0.5 -> coverage * 0.5
            else -> maxOf(joined, coverage)
        }.coerceIn(0.0, 1.0)
    }

    /** The best score over every text a result is known by (title + alternative titles). */
    fun score(query: String, texts: List<String>): Double {
        var best = 0.0
        for (text in texts) {
            if (best >= 1.0) break
            val candidate = similarity(query, text)
            if (candidate > best) best = candidate
        }
        return best
    }

    /**
     * Alternative spellings worth asking the server for when the first answer had nothing close
     * to the query. Ordered by how likely they are to help: each word on its own (the site's
     * fuzzy search happily completes "mieruko" into "Mieruko-chan"), the words squashed together,
     * then shorter prefixes and single-character typos of the longest word.
     */
    fun candidateQueries(query: String, limit: Int = 12): List<String> {
        val normalized = normalize(query)
        val words = tokens(normalized)
        if (words.isEmpty()) return emptyList()

        val candidates = LinkedHashSet<String>()
        fun offer(candidate: String) {
            val clean = candidate.trim()
            if (clean.isNotEmpty() && normalize(clean) != normalized) candidates += clean
        }

        val byLength = words.sortedByDescending { it.length }
        // 1. Each word on its own. The server's fuzzy search completes "mieruko" into
        //    "Mieruko-chan", so a query that trails off ("mieruko-ct") is best answered by its
        //    most distinctive word.
        byLength.filter { it.length >= 4 }.forEach { offer(it) }

        val longest = byLength.first()
        val longestIndex = words.indexOf(longest)
        fun withLongestReplaced(replacement: String) {
            offer(words.toMutableList().also { it[longestIndex] = replacement }.joinToString(" "))
        }

        // 2. Spaces the user left out (or added): "solo leveling" -> "sololeveling".
        if (words.size > 1) offer(words.joinToString(""))

        // 3. A word that was cut short or mistyped near its end ("naruto shippu" -> "naruto
        //    shipp" -> "naruto ship"): the server completes a correct prefix.
        for (cut in 1..3) {
            if (longest.length - cut >= 4) withLongestReplaced(longest.dropLast(cut))
        }

        // 4. Adjacent transpositions, the most common typo of all ("peice" -> "piece",
        //    "nartuo" -> "naruto"). Replacing the word inside the query keeps the rest intact,
        //    so "one peice" is retried as "one piece".
        var swaps = 0
        for (index in 0 until longest.length - 1) {
            if (longest[index] == longest[index + 1]) continue
            if (swaps >= 6) break
            swaps++
            withLongestReplaced(
                longest.substring(0, index) +
                    longest[index + 1] +
                    longest[index] +
                    longest.substring(index + 2),
            )
        }

        // 5. A letter typed twice: "narutto" -> "naruto".
        var collapses = 0
        for (index in 0 until longest.length - 1) {
            if (longest[index] != longest[index + 1]) continue
            if (collapses >= 3) break
            collapses++
            withLongestReplaced(longest.removeRange(index, index + 1))
        }

        return candidates.take(limit)
    }

    /**
     * Final pass over a result set:
     *
     *  * nothing remotely similar (a search for an author, a description keyword, a font of
     *    junk) - the server's own answer is returned untouched, because it may have matched a
     *    field this scoring does not model,
     *  * otherwise every result that is clearly worse than the best match is removed and the
     *    rest is sorted by how well it matches the query.
     *
     * The sort is stable, so results with an equal score keep the server's order.
     */
    fun <T> rank(query: String, items: List<T>, texts: (T) -> List<String>): List<T> {
        if (items.size < 2) return items
        val scored = items.map { it to score(query, texts(it)) }
        val best = scored.maxOf { it.second }
        if (best < WEAK) return items

        val threshold = minOf(KEEP, best)
        return scored
            .filter { it.second >= threshold }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    /**
     * Optimal string alignment distance (Levenshtein plus adjacent transpositions), abandoned as
     * soon as it is certain to exceed [cap] - titles are short, but there are a lot of them.
     */
    internal fun editDistance(a: String, b: String, cap: Int): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        if (kotlin.math.abs(a.length - b.length) > cap) return cap + 1

        var previousPrevious = IntArray(b.length + 1)
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)

        for (i in 1..a.length) {
            current[0] = i
            var rowMinimum = current[0]
            for (j in 1..b.length) {
                val substitution = if (a[i - 1] == b[j - 1]) 0 else 1
                var value = minOf(
                    previous[j] + 1,
                    current[j - 1] + 1,
                    previous[j - 1] + substitution,
                )
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    value = minOf(value, previousPrevious[j - 2] + 1)
                }
                current[j] = value
                if (value < rowMinimum) rowMinimum = value
            }
            if (rowMinimum > cap) return cap + 1
            val recycled = previousPrevious
            previousPrevious = previous
            previous = current
            current = recycled
        }
        return previous[b.length]
    }

    private fun tokenSimilarity(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.length >= 3 && b.startsWith(a)) return 0.95
        if (b.length >= 3 && a.startsWith(b)) return 0.95
        if (a.length >= 3 && b.contains(a)) return 0.90
        val distance = editDistance(a, b, 6)
        val longest = maxOf(a.length, b.length)
        return (1.0 - distance.toDouble() / longest).coerceIn(0.0, 1.0)
    }
}

// ---------------------------------------------------------------------------
// SManga adapters. Kept deliberately thin: everything that matters is scored in [SmartSearch],
// which takes plain strings so that it is fully covered by the unit tests (the real
// SManga.create() only exists inside the reader app, not in the test classpath).
// ---------------------------------------------------------------------------

/** The texts a result is known by: its title plus every alternative title. */
internal fun SManga.searchTexts(): List<String> = listOf(title) + memoAltTitles()

/** Score of a search result against the query. */
internal fun scoreManga(query: String, manga: SManga): Double =
    SmartSearch.score(query, manga.searchTexts())

/** Filter + relevance ordering of a result set. */
internal fun rankMangas(query: String, items: List<SManga>): List<SManga> =
    SmartSearch.rank(query, items) { it.searchTexts() }
