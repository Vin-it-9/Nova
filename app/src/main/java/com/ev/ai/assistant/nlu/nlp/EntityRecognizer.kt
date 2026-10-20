package com.ev.ai.assistant.nlu.nlp

import com.ev.ai.assistant.nlu.EvIntent
import java.util.Locale
import java.util.regex.Pattern

/**
 * Lightweight Named Entity Recognition (NER).
 *
 * Extracts typed entities from raw queries using regex + keyword context.
 * Entity types: TIME, DATE, DURATION, NUMBER, CONTACT, APP, LOCATION, PERCENT,
 * TEMPERATURE, CURRENCY, WEATHER_QUERY, MUSIC_QUERY, REMINDER_TITLE, NOTE_CONTENT.
 *
 * All regex-based — no external ML models — so extraction is instant and works offline.
 */
class EntityRecognizer {

    data class Entity(
        val type: String,
        val value: String,
        val normalized: Any?,
        val start: Int,
        val end: Int,
        val confidence: Double = 1.0
    )

    // NOTE: these are Kotlin raw strings ("""..."""), so regex escapes must be
    // written once (e.g. \b), NOT doubled (\\b) — doubling matches a literal '\b'.

    // ── Time patterns ──
    private val timeRegex = Pattern.compile(
        """\b(\d{1,2})(:(\d{2}))?\s*(am|pm|a\.m\.|p\.m\.)?\b""",
        Pattern.CASE_INSENSITIVE
    )
    private val timeKeywords = setOf("at", "around", "by", "for", "until", "before", "after")

    // ── Date patterns ──
    private val dateRegex = Pattern.compile(
        """\b(today|tomorrow|yesterday|tonight|next\s+week|this\s+weekend|next\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday))\b""",
        Pattern.CASE_INSENSITIVE
    )

    // ── Duration patterns ──
    private val durationRegex = Pattern.compile(
        """\b(\d+)\s*(second|sec|secs|minute|min|mins|hour|hr|hrs|day|days)\b""",
        Pattern.CASE_INSENSITIVE
    )

    // ── Number patterns ──
    private val numberRegex = Pattern.compile(
        """\b(\d+(?:\.\d+)?)\b"""
    )

    // ── Percentage ──
    // No trailing \b: "80%" ends the string, and '%' is a non-word char so a
    // word boundary would never match there.
    private val percentRegex = Pattern.compile(
        """\b(\d+(?:\.\d+)?)\s*(%|percent|percentage)""",
        Pattern.CASE_INSENSITIVE
    )

    // ── Temperature ──
    private val tempRegex = Pattern.compile(
        """\b(\d+(?:\.\d+)?)\s*(°?\s*(c|celsius|celcius|f|fahrenheit))\b""",
        Pattern.CASE_INSENSITIVE
    )

    // ── Unit conversion ──
    private val unitPattern = Pattern.compile(
        """\b(\d+(?:\.\d+)?)\s*([a-zA-Z°]+)\s*(?:to|in|into)\s*([a-zA-Z°]+)\b""",
        Pattern.CASE_INSENSITIVE
    )

    // ── Known app names (expanded dictionary) ──
    private val knownApps = listOf(
        "whatsapp", "youtube", "chrome", "google chrome", "camera", "gallery", "calculator",
        "clock", "settings", "spotify", "instagram", "maps", "google maps", "gmail",
        "play store", "google play", "messages", "telegram", "twitter", "x", "netflix",
        "facebook", "snapchat", "tiktok", "phone", "contacts", "photos", "drive",
        "google drive", "zoom", "linkedin", "calendar", "reminder", "notes", "weather",
        "news", "music", "spotify", "soundcloud", "amazon", "flipkart", "banking",
        "uber", "ola", "zomato", "swiggy", "paytm", "phonepe", "google pay"
    )

    // ── Known contact hints ──
    private val contactIndicators = setOf("call", "dial", "phone", "ring", "sms", "text", "message", "whatsapp", "email", "mail")

    // ── Location / weather hints ──
    private val locationIndicators = setOf("in", "at", "for", "city", "town", "weather", "forecast")

    /**
     * Extract entities from a raw query.
     * Returns map of entityType -> list of entity values (strings).
     */
    fun extract(rawQuery: String, tokens: List<String>, intent: String?): Map<String, List<String>> {
        val result = mutableMapOf<String, MutableList<String>>()

        extractTime(rawQuery, result)
        extractDate(rawQuery, result)
        extractDuration(rawQuery, result)
        extractNumbers(rawQuery, result)
        extractPercentages(rawQuery, result)
        extractTemperature(rawQuery, result)
        extractUnits(rawQuery, result)
        extractApps(rawQuery, tokens, result)
        extractContacts(rawQuery, tokens, intent, result)
        extractLocations(rawQuery, tokens, intent, result)
        extractMusicQuery(rawQuery, tokens, intent, result)
        extractReminderTitle(rawQuery, tokens, intent, result)

        return result.mapValues { it.value.toList() }
    }

    private fun add(result: MutableMap<String, MutableList<String>>, type: String, value: String) {
        result.getOrPut(type) { mutableListOf() }.add(value)
    }

    // ── Time ──
    private fun extractTime(rawQuery: String, result: MutableMap<String, MutableList<String>>) {
        val matcher = timeRegex.matcher(rawQuery)
        while (matcher.find()) {
            val hour = matcher.group(1).toIntOrNull() ?: continue
            val minute = matcher.group(3)?.toIntOrNull() ?: 0
            val ampm = matcher.group(4)?.lowercase(Locale.ROOT) ?: ""
            var h = hour
            when {
                ampm in setOf("pm", "p.m.") && h < 12 -> h += 12
                ampm in setOf("am", "a.m.") && h == 12 -> h = 0
            }
            if (h in 0..23 && minute in 0..59) {
                add(result, "TIME", "${String.format("%02d", h)}:${String.format("%02d", minute)}")
            }
        }
        // Contextual time phrases
        for (kw in timeKeywords) {
            if (rawQuery.lowercase(Locale.ROOT).contains(kw)) {
                // The actual time value is captured by regex above; keyword just marks context
            }
        }
    }

    // ── Date ──
    private fun extractDate(rawQuery: String, result: MutableMap<String, MutableList<String>>) {
        val matcher = dateRegex.matcher(rawQuery)
        while (matcher.find()) {
            add(result, "DATE", matcher.group(1).lowercase(Locale.ROOT))
        }
    }

    // ── Duration ──
    private fun extractDuration(rawQuery: String, result: MutableMap<String, MutableList<String>>) {
        val matcher = durationRegex.matcher(rawQuery)
        while (matcher.find()) {
            val amount = matcher.group(1).toIntOrNull() ?: continue
            val unit = matcher.group(2).lowercase(Locale.ROOT)
            val seconds = when {
                unit.startsWith("h") -> amount * 3600
                unit.startsWith("m") -> amount * 60
                unit.startsWith("s") -> amount
                unit.startsWith("d") -> amount * 86400
                else -> amount * 60
            }
            add(result, "DURATION", seconds.toString())
            add(result, "DURATION_RAW", matcher.group(0))
        }
    }

    // ── Numbers ──
    private fun extractNumbers(rawQuery: String, result: MutableMap<String, MutableList<String>>) {
        val matcher = numberRegex.matcher(rawQuery)
        while (matcher.find()) {
            add(result, "NUMBER", matcher.group(1))
        }
    }

    // ── Percentages ──
    private fun extractPercentages(rawQuery: String, result: MutableMap<String, MutableList<String>>) {
        val matcher = percentRegex.matcher(rawQuery)
        while (matcher.find()) {
            add(result, "PERCENT", matcher.group(1))
        }
    }

    // ── Temperature ──
    private fun extractTemperature(rawQuery: String, result: MutableMap<String, MutableList<String>>) {
        val matcher = tempRegex.matcher(rawQuery)
        while (matcher.find()) {
            add(result, "TEMPERATURE", matcher.group(0))
        }
    }

    // ── Unit conversion ("5 km to miles", "20 celsius to fahrenheit") ──
    private fun extractUnits(rawQuery: String, result: MutableMap<String, MutableList<String>>) {
        val matcher = unitPattern.matcher(rawQuery)
        if (matcher.find()) {
            add(result, "CONVERT_VALUE", matcher.group(1))
            add(result, "FROM_UNIT", matcher.group(2).lowercase(Locale.ROOT))
            add(result, "TO_UNIT", matcher.group(3).lowercase(Locale.ROOT))
        }
    }

    // ── Apps ──
    private fun extractApps(rawQuery: String, tokens: List<String>, result: MutableMap<String, MutableList<String>>) {
        val low = rawQuery.lowercase(Locale.ROOT)
        for (app in knownApps) {
            if (low.contains(app)) {
                add(result, "APP", app)
            }
        }
        // Fuzzy fallback only for reasonably long tokens, with a high threshold,
        // to avoid noise like "set"->"settings" or "new"->"news".
        for (token in tokens) {
            if (token.length < 5) continue
            val (best, score) = FuzzyMatcher.bestMatch(token, knownApps)
            if (score >= 0.90) {
                add(result, "APP", best)
            }
        }
    }

    // ── Contacts ──
    private fun extractContacts(rawQuery: String, tokens: List<String>, intent: String?, result: MutableMap<String, MutableList<String>>) {
        val isComm = intent?.startsWith("comm.") == true
        if (!isComm) return

        val low = rawQuery.lowercase(Locale.ROOT)
        // Patterns like "call john", "text mary", "whatsapp to sarah"
        val contactPattern = Pattern.compile(
            """\b(?:call|dial|phone|ring|text|sms|message|whatsapp|email|mail)\s+(?:to\s+)?([a-zA-Z][a-zA-Z\s]{1,25})""",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = contactPattern.matcher(rawQuery)
        if (matcher.find()) {
            var name = matcher.group(1).trim()
            // Drop trailing command/filler words that the greedy capture may include.
            name = name.replace(
                Regex("\\s+(please|now|today|tomorrow|ok|okay|and)$", RegexOption.IGNORE_CASE),
                ""
            ).trim()
            if (name.isNotBlank()) add(result, "CONTACT", name)
        }
    }

    // ── Locations ──
    private fun extractLocations(rawQuery: String, tokens: List<String>, intent: String?, result: MutableMap<String, MutableList<String>>) {
        val isWeatherOrNav = intent?.let { it.startsWith("info.") || it.startsWith("nav.") } == true
        if (!isWeatherOrNav) return

        val low = rawQuery.lowercase(Locale.ROOT)
        // "weather in london", "directions to new york", "weather for paris"
        val locPattern = Pattern.compile(
            """\b(?:in|at|for|to)\s+([a-zA-Z][a-zA-Z\s]{1,30})""",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = locPattern.matcher(rawQuery)
        if (matcher.find()) {
            var loc = matcher.group(1).trim()
            loc = loc.replace(
                Regex("\\s+(please|now|today|tomorrow)$", RegexOption.IGNORE_CASE),
                ""
            ).trim()
            if (loc.isNotBlank()) add(result, "LOCATION", loc)
        }
    }

    // ── Music query ──
    private fun extractMusicQuery(rawQuery: String, tokens: List<String>, intent: String?, result: MutableMap<String, MutableList<String>>) {
        if (intent?.startsWith("media.") != true) return
        val low = rawQuery.lowercase(Locale.ROOT)
        val musicIndicators = setOf("play", "song", "artist", "playlist", "music", "track")
        val hasIndicator = musicIndicators.any { low.contains(it) }
        if (!hasIndicator) return

        // Strip the command verb to leave the query
        var query = low
        for (word in listOf("play", "song", "artist", "playlist", "music", "track", "resume", "start", "pause", "stop", "next", "previous", "prev", "back")) {
            query = query.removePrefix(word).trim()
        }
        if (query.isNotBlank()) add(result, "MUSIC_QUERY", query)
    }

    // ── Reminder title ──
    private fun extractReminderTitle(rawQuery: String, tokens: List<String>, intent: String?, result: MutableMap<String, MutableList<String>>) {
        if (intent != "productivity.create_reminder") return
        var title = rawQuery.lowercase(Locale.ROOT)

        // Strip leading command phrases (longest first).
        val prefixes = listOf(
            "remind me to", "remind me that", "remind me", "create a reminder to",
            "create reminder to", "create reminder", "set a reminder to", "set reminder",
            "remember to", "new reminder", "add reminder", "reminder to", "reminder"
        )
        for (p in prefixes) {
            if (title.startsWith(p)) { title = title.removePrefix(p).trim(); break }
        }

        // Remove time expressions anywhere (e.g. "at 5:00", "7pm", "by 9").
        title = Regex("""\b(at|by|around|on|for)\s+\d{1,2}(:\d{2})?\s*(am|pm)?\b""", RegexOption.IGNORE_CASE)
            .replace(title, " ")
        title = Regex("""\b\d{1,2}(:\d{2})?\s*(am|pm)\b""", RegexOption.IGNORE_CASE).replace(title, " ")
        title = Regex("""\b\d{1,2}:\d{2}\b""").replace(title, " ")

        // Remove leftover date words and dangling trailing fillers.
        title = Regex("""\b(today|tomorrow|tonight|yesterday|next week|this weekend)\b""", RegexOption.IGNORE_CASE)
            .replace(title, " ")
        title = title.replace(Regex("""\s+(at|on|by|for|in)$""", RegexOption.IGNORE_CASE), " ")
        title = title.replace(Regex("""\s+"""), " ").trim().trim(',', '.', '-')

        if (title.isNotBlank()) add(result, "REMINDER_TITLE", title)
    }

    /** Convenience: get first entity of a type as string. */
    fun first(entities: Map<String, List<String>>, type: String): String? =
        entities[type]?.firstOrNull()

    /** Convenience: get first duration in seconds. */
    fun firstDurationSeconds(entities: Map<String, List<String>>): Int? =
        entities["DURATION"]?.firstOrNull()?.toIntOrNull()
}