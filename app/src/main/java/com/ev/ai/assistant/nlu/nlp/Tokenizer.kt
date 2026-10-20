package com.ev.ai.assistant.nlu.nlp

import java.text.BreakIterator
import java.util.Locale

/**
 * Lightweight, zero-dependency tokenizer for the NLU engine.
 *
 * Splits text into words/sentences using ICU break iterators so it handles
 * Unicode correctly without pulling in heavy NLP libraries. Optimized for
 * sub-millisecond execution on short voice queries.
 */
object Tokenizer {

    private val wordIterator = BreakIterator.getWordInstance(Locale.ROOT)
    private val sentenceIterator = BreakIterator.getSentenceInstance(Locale.ROOT)

    /** Splits text into lowercase word tokens, stripping punctuation. */
    fun tokenize(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val tokens = mutableListOf<String>()
        wordIterator.setText(text)
        var start = wordIterator.first()
        var end = wordIterator.next()
        while (end != BreakIterator.DONE) {
            val candidate = text.substring(start, end).trim()
            if (candidate.isNotEmpty() && candidate.any { it.isLetterOrDigit() }) {
                tokens.add(candidate.lowercase(Locale.ROOT))
            }
            start = end
            end = wordIterator.next()
        }
        return tokens
    }

    /** Splits text into sentences. */
    fun splitSentences(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val sentences = mutableListOf<String>()
        sentenceIterator.setText(text)
        var start = sentenceIterator.first()
        var end = sentenceIterator.next()
        while (end != BreakIterator.DONE) {
            val sentence = text.substring(start, end).trim()
            if (sentence.isNotEmpty()) sentences.add(sentence)
            start = end
            end = sentenceIterator.next()
        }
        return sentences
    }

    /** Returns n-grams (space-joined) of size n from the token list. */
    fun ngrams(tokens: List<String>, n: Int): List<String> {
        if (tokens.isEmpty() || n <= 0 || n > tokens.size) return emptyList()
        return (0 until tokens.size - n + 1).map { i ->
            tokens.subList(i, i + n).joinToString(" ")
        }
    }

    /** All n-grams from size 1 up to maxN. */
    fun allNgrams(tokens: List<String>, maxN: Int): List<String> {
        val result = mutableListOf<String>()
        for (n in 1..maxN) result.addAll(ngrams(tokens, n))
        return result
    }
}