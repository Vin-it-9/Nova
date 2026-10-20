package com.ev.ai.assistant.nlu.nlp

/**
 * Calculates a confidence score (0.0 to 1.0) for an intent match based on
 * keyword coverage, regex strength, fuzzy similarity, and entity completeness.
 */
object ConfidenceScorer {

    data class Signal(
        val keywordCoverage: Double = 0.0, // fraction of intent keywords found
        val regexMatch: Boolean = false,
        val fuzzySimilarity: Double = 0.0,
        val entityCompleteness: Double = 1.0, // how many required entities found
        val exactPhrase: Boolean = false,
        val lengthPenalty: Double = 1.0     // penalty for very short/ambiguous queries
    )

    fun score(signal: Signal): Double {
        var score = 0.0
        score += signal.keywordCoverage * 0.30
        score += (if (signal.regexMatch) 0.25 else 0.0)
        score += signal.fuzzySimilarity * 0.20
        score += signal.entityCompleteness * 0.15
        score += (if (signal.exactPhrase) 0.10 else 0.0)
        score *= signal.lengthPenalty
        return score.coerceIn(0.0, 1.0)
    }

    fun lengthPenalty(queryLength: Int): Double = when {
        queryLength <= 2 -> 0.6      // "hi", "ok" - ambiguous
        queryLength <= 5 -> 0.85
        else -> 1.0
    }
}