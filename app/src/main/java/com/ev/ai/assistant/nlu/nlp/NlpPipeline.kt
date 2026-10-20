package com.ev.ai.assistant.nlu.nlp

/**
 * Orchestrates the full NLP pipeline:
 *   normalize -> tokenize -> classify intent -> extract entities -> score confidence
 */
data class NlpResult(
    val rawQuery: String,
    val normalized: String,
    val tokens: List<String>,
    val contentTokens: List<String>,
    val intent: String?,
    val intentConfidence: Double,
    val entities: Map<String, List<String>>,
    val allIntentScores: Map<String, Double>
)

object NlpPipeline {

    fun analyze(
        rawQuery: String,
        intentClassifier: IntentClassifier,
        entityRecognizer: EntityRecognizer
    ): NlpResult {
        val normalized = Normalizer.normalize(rawQuery)
        val tokens = Tokenizer.tokenize(normalized)
        val contentTokens = Normalizer.contentTokens(rawQuery)

        // Keep function words for intent matching: commands such as "turn on Wi-Fi",
        // "set a timer", and "call Mom" use those tokens as meaningful signals.
        val classification = intentClassifier.classify(tokens, rawQuery)
        val entities = entityRecognizer.extract(rawQuery, tokens, classification.first)

        return NlpResult(
            rawQuery = rawQuery,
            normalized = normalized,
            tokens = tokens,
            contentTokens = contentTokens,
            intent = classification.first,
            intentConfidence = classification.second,
            entities = entities,
            allIntentScores = classification.third
        )
    }
}

/** Result of intent classification: (intentName, confidence, allScores). */
typealias ClassificationResult = Triple<String, Double, Map<String, Double>>