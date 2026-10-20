package com.ev.ai.assistant.nlu.nlp

import java.text.Normalizer
import java.util.Locale

/**
 * Text normalizer: lowercasing, accent stripping, punctuation removal,
 * light stemming, and stopword filtering. Fast and dependency-free.
 */
object Normalizer {

    private val stopwords = setOf(
        "a", "an", "the", "is", "are", "am", "was", "were", "be", "been", "being",
        "have", "has", "had", "do", "does", "did", "will", "would", "could", "should",
        "may", "might", "must", "shall", "can", "need", "dare", "ought", "used",
        "to", "of", "in", "for", "on", "with", "at", "by", "from", "up", "about",
        "into", "through", "during", "before", "after", "above", "below", "between",
        "under", "again", "further", "then", "once", "here", "there", "when", "where",
        "why", "how", "all", "each", "few", "more", "most", "other", "some", "such",
        "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very", "s",
        "t", "just", "don", "now", "i", "me", "my", "myself", "we", "our", "ours",
        "ourselves", "you", "your", "yours", "yourself", "yourselves", "he", "him",
        "his", "himself", "she", "her", "hers", "herself", "it", "its", "itself",
        "they", "them", "their", "theirs", "themselves", "what", "which", "who",
        "whom", "this", "that", "these", "those", "and", "but", "if", "or", "because",
        "as", "until", "while", "of", "at", "by", "for", "with", "about", "against",
        "please", "can you", "could you", "would you", "hey", "hi", "hello", "ok",
        "okay", "ev", "jarvis", "friday", "edith", "assistant"
    )

    /** Strip diacritics/accents (e.g. café -> cafe). */
    fun stripAccents(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("[\\p{InCombiningDiacriticalMarks}]"), "")
            .replace(Regex("[^\\p{ASCII}]"), "")

    /** Remove punctuation but keep spaces and alphanumerics. */
    fun stripPunctuation(text: String): String =
        text.replace(Regex("[^\\p{L}\\p{N}\\s]"), " ").replace(Regex("\\s+"), " ").trim()

    /** Remove stopwords from a token list. */
    fun removeStopwords(tokens: List<String>): List<String> =
        tokens.filter { it !in stopwords }

    /** Very light heuristic stemmer (removes common suffixes). Not perfect but fast. */
    fun stem(token: String): String = when {
        token.length > 4 && token.endsWith("ing") -> token.dropLast(3)
        token.length > 4 && token.endsWith("ed") -> token.dropLast(2)
        token.length > 3 && token.endsWith("s") && !token.endsWith("ss") -> token.dropLast(1)
        token.length > 3 && token.endsWith("ly") -> token.dropLast(2)
        else -> token
    }

    /** Full normalization pipeline for a raw query string. */
    fun normalize(text: String): String {
        var t = text.lowercase(Locale.ROOT)
        t = stripAccents(t)
        t = stripPunctuation(t)
        return t
    }

    /** Normalize and return meaningful content tokens (stopwords removed, stemmed). */
    fun contentTokens(text: String): List<String> {
        val normalized = normalize(text)
        val tokens = Tokenizer.tokenize(normalized)
        return removeStopwords(tokens).map { stem(it) }
    }

    /** Check if a token is a stopword. */
    fun isStopword(token: String): Boolean = token in stopwords
}