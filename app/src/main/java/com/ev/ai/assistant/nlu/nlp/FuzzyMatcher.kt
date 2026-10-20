package com.ev.ai.assistant.nlu.nlp

/**
 * Fast fuzzy string matching using Levenshtein distance and Jaro-Winkler similarity.
 *
 * Used to match user phrasing against known intent patterns and entity values
 * when exact keyword matching fails. Optimized for short strings (< 60 chars)
 * typical of voice commands.
 */
object FuzzyMatcher {

    /** Levenshtein edit distance between two strings. */
    fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val row = IntArray(b.length + 1)
        for (j in 0..b.lastIndex) row[j] = j

        for (i in 1..a.lastIndex) {
            val prev = row[0]
            row[0] = i
            var prevDiagonal = prev
            for (j in 1..b.length) {
                val current = row[j]
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                row[j] = minOf(
                    row[j] + 1,        // deletion
                    row[j - 1] + 1,    // insertion
                    prevDiagonal + cost // substitution
                )
                prevDiagonal = current
            }
        }
        return row[b.length]
    }

    /** Similarity score 0.0 (no match) to 1.0 (exact match). */
    fun similarity(a: String, b: String): Double {
        if (a.isEmpty() && b.isEmpty()) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val dist = levenshtein(a, b)
        val maxLen = maxOf(a.length, b.length)
        return 1.0 - (dist.toDouble() / maxLen)
    }

    /**
     * Jaro-Winkler similarity — better for short strings and typos.
     * Returns 0.0 to 1.0.
     */
    fun jaroWinkler(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0

        val matchWindow = maxOf(s1.length, s2.length) / 2 - 1
        val s1Matches = BooleanArray(s1.length)
        val s2Matches = BooleanArray(s2.length)

        var matches = 0
        for (i in s1.indices) {
            val start = maxOf(0, i - matchWindow)
            val end = minOf(i + matchWindow + 1, s2.length)
            for (j in start until end) {
                if (!s2Matches[j] && s1[i] == s2[j]) {
                    s1Matches[i] = true
                    s2Matches[j] = true
                    matches++
                    break
                }
            }
        }

        if (matches == 0) return 0.0

        var transpositions = 0
        var k = 0
        for (i in s1.indices) {
            if (s1Matches[i]) {
                while (!s2Matches[k]) k++
                if (s1[i] != s2[k]) transpositions++
                k++
            }
        }

        val jaro = (matches.toDouble() / s1.length +
            matches.toDouble() / s2.length +
            (matches - transpositions / 2).toDouble() / matches) / 3.0

        // Winkler boost for common prefix (up to 4 chars)
        var prefix = 0
        for (i in 0 until minOf(4, minOf(s1.length, s2.length))) {
            if (s1[i] == s2[i]) prefix++ else break
        }
        val p = 0.1 // scaling factor
        return jaro + prefix * p * (1.0 - jaro)
    }

    /** Best match score (0-1) of query against a list of candidate phrases. */
    fun bestMatch(query: String, candidates: List<String>): Pair<String, Double> {
        if (candidates.isEmpty()) return "" to 0.0
        var best = "" to 0.0
        val q = query.lowercase()
        for (c in candidates) {
            val s = jaroWinkler(q, c.lowercase())
            if (s > best.second) best = c to s
        }
        return best
    }

    /** All matches above a threshold, sorted by score descending. */
    fun matchesAbove(
        query: String,
        candidates: List<String>,
        threshold: Double = 0.75
    ): List<Pair<String, Double>> {
        val q = query.lowercase()
        return candidates
            .map { c -> c to jaroWinkler(q, c.lowercase()) }
            .filter { it.second >= threshold }
            .sortedByDescending { it.second }
    }
}