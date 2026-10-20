package com.ev.ai.assistant.nlu

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/**
 * Small, dependency-free web lookups used for information intents.
 *
 * Everything here hits a free, key-less endpoint and returns plain text that can be
 * spoken directly, so the assistant never has to fall back to a canned/hardcoded
 * answer. All calls are blocking and must therefore run off the main thread.
 */
object EvWeb {

    private const val TAG = "EvWeb"
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Mobile Safari/537.36"

    private fun get(url: String, timeoutMs: Int = 8000): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "*/*")
            }
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.w(TAG, "GET failed ($url): ${e.message}")
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    /** Like [enc] but safe for URL path segments ("New York" -> "New%20York"). */
    private fun encPath(s: String): String = enc(s).replace("+", "%20")

    /**
     * Answers a general-knowledge question from the web so the assistant never has to
     * fall back to a canned identity line.
     *
     * Tries DuckDuckGo's instant-answer API first, then falls back to a Wikipedia
     * article summary. Returns null when neither source has anything to say.
     */
    fun answer(query: String): String? {
        val q = query.trim()
        if (q.isBlank()) return null
        instantAnswer(q)?.let { return it }
        return wikipediaSummary(q)
    }

    /** DuckDuckGo Instant Answer (no key required). */
    private fun instantAnswer(query: String): String? {
        val url = "https://api.duckduckgo.com/?q=${enc(query)}&format=json&no_html=1&skip_disambig=1"
        val body = get(url) ?: return null
        return parseInstantAnswer(body)
    }

    /**
     * Extracts the most useful human-readable answer from a DuckDuckGo instant-answer
     * payload. Pure string parsing so it can be unit tested without a network.
     */
    internal fun parseInstantAnswer(json: String): String? {
        return try {
            val root = JSONObject(json)
            val abstractText = root.optString("AbstractText").trim()
            if (abstractText.isNotBlank()) return abstractText
            val answer = root.optString("Answer").trim()
            if (answer.isNotBlank()) return answer
            val definition = root.optString("Definition").trim()
            if (definition.isNotBlank()) return definition
            val related = root.optJSONArray("RelatedTopics")
            if (related != null) {
                for (i in 0 until related.length()) {
                    val item = related.optJSONObject(i) ?: continue
                    val text = item.optString("Text").trim()
                    if (text.isNotBlank()) return text
                    val first = item.optJSONArray("FirstAbsoluteText")
                    val firstText = first?.optString(0)?.trim()
                    if (!firstText.isNullOrBlank()) return firstText
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "instant answer parse failed: ${e.message}")
            null
        }
    }

    /** Cleans leading question words so "who is the president" searches for "president". */
    internal fun cleanKnowledgeQuery(query: String): String {
        var s = query.lowercase(Locale.ROOT).trim().trimEnd('?', '!', '.')
        val prefixes = listOf(
            "who is ", "who was ", "who were ", "who are ", "whose ",
            "what is ", "what was ", "what are ", "what's ", "whats ",
            "when is ", "when was ", "when were ", "where is ", "where was ",
            "why is ", "why was ", "how many ", "how much ", "how far ",
            "how old ", "how tall ", "tell me about ", "explain ", "define ",
            "the president of ", "president of "
        )
        var changed = true
        while (changed) {
            changed = false
            for (p in prefixes) {
                if (s.startsWith(p)) {
                    s = s.removePrefix(p).trim()
                    changed = true
                    break
                }
            }
        }
        return s
    }

    /** Wikipedia article summary for [query], via the opensearch + summary endpoints. */
    private fun wikipediaSummary(query: String): String? {
        val term = cleanKnowledgeQuery(query)
        if (term.isBlank()) return null
        val searchUrl =
            "https://en.wikipedia.org/w/api.php?action=opensearch&search=${enc(term)}&limit=1&format=json"
        val searchBody = get(searchUrl) ?: return null
        val title = try {
            JSONArray(searchBody).optJSONArray(1)?.optString(0)?.trim()?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.w(TAG, "wikipedia search parse failed: ${e.message}")
            null
        } ?: return null
        val summaryBody = get("https://en.wikipedia.org/api/rest_v1/page/summary/${encPath(title)}")
            ?: return null
        return try {
            JSONObject(summaryBody).optString("extract").trim().takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.w(TAG, "wikipedia summary parse failed: ${e.message}")
            null
        }
    }

    /** Current weather. wttr.in geolocates automatically when no location is given. */
    fun weather(location: String?): String? {
        val loc = location?.trim()?.takeIf { it.isNotBlank() }
        val url = if (loc != null) {
            "https://wttr.in/${enc(loc)}?format=%l:+%C,+%t,+feels+like+%f,+humidity+%h"
        } else {
            "https://wttr.in/?format=%l:+%C,+%t,+feels+like+%f,+humidity+%h"
        }
        val body = get(url)?.trim() ?: return null
        if (body.isBlank() || body.contains("Unknown location", ignoreCase = true)) return null
        return body
    }

    /** First couple of dictionary definitions for [word]. */
    fun definition(word: String): String? {
        val w = word.trim().split(Regex("\\s+")).firstOrNull { it.isNotBlank() } ?: return null
        val body = get("https://api.dictionaryapi.dev/api/v2/entries/en/${enc(w)}") ?: return null
        return try {
            val entries = JSONArray(body)
            val meanings = entries.getJSONObject(0).getJSONArray("meanings")
            val defs = mutableListOf<String>()
            for (i in 0 until meanings.length()) {
                val meaning = meanings.getJSONObject(i)
                val pos = meaning.optString("partOfSpeech")
                val defsArr = meaning.optJSONArray("definitions") ?: continue
                if (defsArr.length() > 0) {
                    val text = defsArr.getJSONObject(0).optString("definition")
                    if (text.isNotBlank()) {
                        defs.add(if (pos.isNotBlank()) "as a $pos, $text" else text)
                    }
                }
                if (defs.size >= 2) break
            }
            if (defs.isEmpty()) null else defs.joinToString(" ")
        } catch (e: Exception) {
            Log.w(TAG, "definition parse failed: ${e.message}")
            null
        }
    }

    /** Translates [text] into [targetLanguage] (English name or ISO code). */
    fun translate(text: String, targetLanguage: String?): String? {
        if (text.isBlank()) return null
        val lang = languageCode(targetLanguage) ?: "es"
        val url = "https://api.mymemory.translated.net/get?q=${enc(text)}&langpair=en|$lang"
        val body = get(url) ?: return null
        return try {
            JSONObject(body).getJSONObject("responseData").optString("translatedText")
                .takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.w(TAG, "translate parse failed: ${e.message}")
            null
        }
    }

    /** Latest price for a stock [symbol] via the Yahoo Finance chart API. */
    fun stock(symbol: String): String? {
        val s = symbol.trim().uppercase(Locale.ROOT).replace(" ", "")
        if (s.isBlank()) return null
        val body = get("https://query1.finance.yahoo.com/v8/finance/chart/${enc(s)}?interval=1d&range=1d")
            ?: return null
        return try {
            val meta = JSONObject(body)
                .getJSONObject("chart")
                .getJSONArray("result")
                .getJSONObject(0)
                .getJSONObject("meta")
            val price = meta.optDouble("regularMarketPrice", Double.NaN)
            if (price.isNaN()) return null
            val name = meta.optString("shortName").takeIf { it.isNotBlank() }
                ?: meta.optString("longName").takeIf { it.isNotBlank() }
                ?: s
            val currency = meta.optString("currency")
            val ticker = meta.optString("symbol").ifBlank { s }
            val priceText = if (currency == "USD") "$$price" else if (currency.isNotBlank()) "$price $currency" else "$price"
            "$name ($ticker) is trading at $priceText."
        } catch (e: Exception) {
            Log.w(TAG, "stock parse failed: ${e.message}")
            null
        }
    }

    /** Top [limit] headlines from Google News RSS. */
    fun news(limit: Int): List<String> {
        val body = get("https://news.google.com/rss?hl=en-IN&gl=IN&ceid=IN:en") ?: return emptyList()
        return parseNewsTitles(body).take(limit)
    }

    /**
     * Extracts item headlines from a Google News RSS document. Titles are matched only
     * inside `<item>` blocks so the feed's own title and the channel image title
     * ("Google News") are not returned as headlines. Exposed for unit testing.
     */
    internal fun parseNewsTitles(xml: String): List<String> =
        Regex(
            "<item>.*?<title>(?:<!\\[CDATA\\[)?(.*?)(?:\\]\\]>)?</title>",
            RegexOption.DOT_MATCHES_ALL
        ).findAll(xml)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotBlank() }
            .toList()

    private fun languageCode(name: String?): String? = when (name?.lowercase()?.trim()) {
        null, "" -> null
        "spanish", "es" -> "es"
        "french", "fr" -> "fr"
        "german", "de" -> "de"
        "italian", "it" -> "it"
        "portuguese", "pt" -> "pt"
        "russian", "ru" -> "ru"
        "japanese", "ja" -> "ja"
        "korean", "ko" -> "ko"
        "chinese", "zh" -> "zh"
        "hindi", "hi" -> "hi"
        "arabic", "ar" -> "ar"
        "dutch", "nl" -> "nl"
        "turkish", "tr" -> "tr"
        "bengali", "bn" -> "bn"
        "tamil", "ta" -> "ta"
        "telugu", "te" -> "te"
        else -> name
    }
}
