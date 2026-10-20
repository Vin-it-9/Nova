package com.ev.ai.assistant.nlu

import android.content.Context
import com.ev.ai.assistant.nlu.nlp.EntityRecognizer
import com.ev.ai.assistant.nlu.nlp.IntentClassifier
import com.ev.ai.assistant.nlu.nlp.NlpPipeline
import com.ev.ai.assistant.nlu.nlp.Normalizer
import com.ev.ai.assistant.nlu.nlp.Tokenizer
import java.util.Locale
import java.util.regex.Pattern

/**
 * High-performance, fully offline Natural Language Understanding (NLU) engine.
 *
 * Pipeline:
 *   1. Wake-word / politeness prefix stripping
 *   2. Normalization & tokenization
 *   3. Intent classification (keyword + regex + fuzzy, weighted scoring)
 *   4. Named entity recognition (time, date, contact, app, location, etc.)
 *   5. Confidence scoring; below threshold -> Unknown with suggestions
 *
 * No TensorFlow Lite / ONNX — pure Kotlin. Sub-millisecond inference on short
 * voice queries, instant startup.
 */
object EvNluEngine {

    // ── Wake-word / politeness prefix stripping ────────────────────────────
    private val WAKE_PREFIXES = listOf(
        Pattern.compile("""^(?:hey|hi|hello|ok|okay)\s+(?:ev|e\.?v\.?|jarvis|friday|edith)\s*[,:]?\s*""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""^(?:ev|e\.?v\.?|jarvis|friday|edith)\s*[,:]?\s*""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""^(?:please|can you|could you|would you)\s+""", Pattern.CASE_INSENSITIVE)
    )

    // Minimum confidence to accept a classified intent
    private const val MIN_CONFIDENCE = 0.45

    private val classifier = IntentClassifier()
    private val entityRecognizer = EntityRecognizer()

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Parse a raw query into a structured [EvIntent].
     *
     * @param rawQuery the user's spoken or typed text
     * @param context  optional Android context (for dynamic app resolution)
     */
    fun parse(rawQuery: String, context: Context? = null): EvIntent {
        val stripped = stripWakePrefixes(rawQuery)
        if (stripped.isBlank()) {
            return EvIntent.Conversation("I'm listening. How can I help you?")
        }

        parseDeterministic(stripped, context)?.let { return it }

        val normalized = Normalizer.normalize(stripped)
        val tokens = Tokenizer.tokenize(normalized)
        val contentTokens = Normalizer.contentTokens(stripped)

        val pipeline = NlpPipeline.analyze(stripped, classifier, entityRecognizer)
        val intentName = pipeline.intent ?: "unknown"
        val confidence = pipeline.intentConfidence
        val entities = pipeline.entities

        return when {
            confidence < MIN_CONFIDENCE -> {
                // Knowledge-shaped questions ("who is the president", "president of usa",
                // "capital of france") have no on-device action; answer them from the web
                // rather than falling back to a canned identity line or a dead end.
                if (looksLikeKnowledgeQuestion(stripped)) {
                    EvIntent.Answer(stripped)
                } else {
                    EvIntent.Unknown(
                        rawQuery = stripped,
                        confidence = confidence,
                        suggestions = buildSuggestions(intentName, confidence, stripped)
                    )
                }
            }
            else -> buildIntent(intentName, entities, stripped, context, confidence)
        }
    }

    /**
     * Handles high-confidence command shapes before fuzzy classification. This prevents
     * generic words such as "set", "on", or "music" from stealing structured commands.
     */
    private fun parseDeterministic(query: String, context: Context?): EvIntent? {
        val low = query.lowercase(Locale.ROOT)

        if (low.contains("identify yourself") || low == "who r u" ||
            low.contains("who are you") || low.contains("what are you") ||
            low.contains("your name") || low.contains("are you ev") ||
            low.contains("are you jarvis")
        ) {
            return EvIntent.Conversation(PersonaResponder.whoAreYou(context))
        }

        if (low == "google maps" || low == "open google maps") {
            return EvIntent.OpenMaps()
        }

        // Navigation. "locate me to satara", "take me to pune", "directions to mumbai",
        // "route to home", "how do i get to the airport". A destination is required;
        // bare "locate me" (no destination) opens Maps centered on the current spot.
        navDestination(low)?.let { dest ->
            return EvIntent.GetDirections(origin = null, destination = dest)
        }
        if (Regex("""^(?:locate|where am i|current location)(?:\s+me)?(?:\s+now)?[.!?]?$""").matches(low)) {
            return EvIntent.OpenMaps()
        }

        // Nearby place search: "hospital near me", "find an atm near me",
        // "nearby hotels", "closest coffee shop".
        nearbyQuery(low)?.let { place -> return EvIntent.NearbySearch(place) }

        // Camera: open front/back camera, take a photo, start a video recording.
        cameraIntent(low)?.let { intent -> return intent }

        if (low.startsWith("search for ") || low.startsWith("google ") ||
            low.startsWith("look up ") || low.startsWith("search google for ")
        ) {
            val queryText = low
                .removePrefix("search google for ")
                .removePrefix("search for ")
                .removePrefix("google ")
                .removePrefix("look up ")
                .trim()
            return EvIntent.SearchWeb(queryText)
        }

        if (low.contains("unusual and random") || low == "do something unusual") {
            return EvIntent.Unknown(query)
        }

        val calculation = Regex(
            """^(?:calculate\s+|compute\s+|solve\s+|what\s+is\s+|how\s+much\s+is\s+)?(\d+(?:\.\d+)?)\s*(\+|-|\*|/|x|×|÷|plus|minus|times|divided by|multiplied by)\s*(\d+(?:\.\d+)?)$"""
        ).find(low)
        if (calculation != null) {
            val left = calculation.groupValues[1].toDouble()
            val operator = calculation.groupValues[2]
            val right = calculation.groupValues[3].toDouble()
            val result = when (operator) {
                "+", "plus" -> left + right
                "-", "minus" -> left - right
                "*", "x", "×", "times", "multiplied by" -> left * right
                "/", "÷", "divided by" -> if (right == 0.0) Double.NaN else left / right
                else -> return null
            }
            return EvIntent.Calculate(query, result)
        }

        val alarm = Regex(
            """^(?:set\s+(?:an?\s+)?alarm\s+(?:for|at)?|wake\s+me(?:\s+up)?\s+at)\s*(\d{1,2})(?::(\d{2}))?\s*(am|pm)?$"""
        ).find(low)
        if (alarm != null) {
            var hour = alarm.groupValues[1].toInt()
            val minute = alarm.groupValues[2].toIntOrNull() ?: 0
            when (alarm.groupValues[3]) {
                "pm" -> if (hour < 12) hour += 12
                "am" -> if (hour == 12) hour = 0
            }
            if (hour in 0..23 && minute in 0..59) {
                return EvIntent.SetAlarm(hour, minute)
            }
        }

        // Flexible alarm shapes the strict regex above misses: "set alarm for 7 30 pm",
        // "wake me up at 7 in the morning", "alarm for quarter past seven", "7 in the
        // evening". Only fires when the phrase clearly asks to set/wake with a time.
        if (!LOW_PRIORITY_ALARM_VERBS.containsMatchIn(low) &&
            (low.contains("alarm") || low.startsWith("wake me") || low.startsWith("wake up"))
        ) {
            parseSpokenTime(low)?.let { (h, m) ->
                if (h in 0..23 && m in 0..59) return EvIntent.SetAlarm(h, m)
            }
        }

        if (low == "snooze" || low == "snooze alarm") return EvIntent.SnoozeAlarm()
        if (low.matches(Regex("^(delete|remove|cancel)\\s+alarm$"))) {
            return EvIntent.DeleteAlarm()
        }
        if (low == "edit alarm") return EvIntent.EditAlarm(0, 0, 0, 0)
        if (low == "check alarm") return EvIntent.CheckAlarm

        if (low.matches(Regex("^(cancel|stop|clear)\\s+(?:the\\s+)?timer$"))) {
            return EvIntent.CancelTimer()
        }
        if (low.matches(Regex("^pause(?:\\s+the)?\\s+timer$"))) return EvIntent.PauseTimer
        if (low.matches(Regex("^(resume|continue)(?:\\s+the)?\\s+timer$"))) return EvIntent.ResumeTimer
        if (low.matches(Regex("^check\\s+(?:the\\s+)?timer$"))) return EvIntent.CheckTimer

        // Network info / connectivity details
        if (low == "network info" || low == "network status" || low == "network details" ||
            low == "internet info" || low == "internet status" || low == "connection info" ||
            low == "connection details" || low == "my ip" || low == "ip address" ||
            low == "wifi info" || low == "mobile data info"
        ) {
            return EvIntent.GetNetworkInfo
        }
        val timer = Regex(
            """^(?:set\s+(?:a\s+)?timer(?:\s+for)?|timer(?:\s+for)?|countdown)\s*(\d+)\s*(hour|hr|minute|min|minutes|mins|second|sec|seconds|secs)$"""
        ).find(low) ?: Regex(
            """^(\d+)\s*(hour|hr|minute|min|minutes|mins|second|sec|seconds|secs)\s+timer$"""
        ).find(low)
        if (timer != null) {
            val amount = timer.groupValues[1].toInt()
            val unit = timer.groupValues[2]
            val seconds = when {
                unit.startsWith("hour") || unit == "hr" -> amount * 3600
                unit.startsWith("min") -> amount * 60
                else -> amount
            }
            return EvIntent.SetTimer(seconds)
        }

        if (low.startsWith("send whatsapp") || low.startsWith("whatsapp")) {
            val message = Regex("""(?:saying|with message|text)\s+(.+)$""")
                .find(low)?.groupValues?.get(1)?.trim()
            return EvIntent.SendWhatsAppMessage(message = message)
        }
        if (low.startsWith("send sms") || low.startsWith("send text")) {
            val match = Regex("""^(?:send\s+(?:sms|text))(?:\s+to)?\s+(\S+)(?:\s+(.+))?$""")
                .find(low)
            if (match != null) {
                return EvIntent.SendSms(match.groupValues[1], match.groupValues[2] ?: "")
            }
        }
        if (low.startsWith("read unread messages")) return EvIntent.ReadMessages(unreadOnly = true)
        if (low == "read messages") return EvIntent.ReadMessages()

        if (low == "pause") return EvIntent.PauseMusic
        if (low == "resume music") return EvIntent.ResumeMusic
        if (low.startsWith("play music") || low.startsWith("play song ") || low.startsWith("play spotify")) {
            val musicQuery = if (low.startsWith("play song ")) {
                low.removePrefix("play song ").takeIf { it.isNotBlank() }
            } else {
                null
            }
            return EvIntent.PlayMusic(musicQuery)
        }
        // "play believer" / "play believer by imagine dragons" — treat anything after
        // "play" as the track/artist to search for so playback actually starts.
        if (low.startsWith("play ")) {
            val subject = low.removePrefix("play ").trim()
                .removePrefix("the ").trim()
            if (subject.isNotBlank() && subject !in setOf("music", "song", "spotify", "something", "a song")) {
                return EvIntent.PlayMusic(subject)
            }
        }
        if (low == "next track" || low == "next song") return EvIntent.NextTrack
        if (low == "previous track") return EvIntent.PreviousTrack
        if (low == "stop music") return EvIntent.StopMusic
        if (low.startsWith("set music volume ")) {
            return EvIntent.SetMusicVolume(low.substringAfterLast(" ").toIntOrNull() ?: 50)
        }

        // Flashlight / torch — handle explicitly so "turn on torch" can never be
        // mistaken for another "turn on ..." toggle (e.g. Bluetooth).
        if (low.contains("torch") || low.contains("flashlight")) {
            val off = low.contains("off") || low.contains("disable")
            val on = low.contains("on") || low.contains("enable")
            return EvIntent.ToggleFlashlight(
                when {
                    on && !off -> true
                    off -> false
                    else -> null
                }
            )
        }

        // Edit/change an existing alarm: "change alarm from 7 to 8 am".
        if (low.matches(Regex("^(edit|change|move|reschedule)\\s+alarm.*"))) {
            val times = Regex("""(\d{1,2})(?::(\d{2}))?\s*(am|pm)?""").findAll(low).toList()
            if (times.isNotEmpty()) {
                fun hm(m: MatchResult): Pair<Int, Int> {
                    var h = m.groupValues[1].toIntOrNull() ?: 7
                    val min = m.groupValues[2].toIntOrNull() ?: 0
                    when (m.groupValues[3]) {
                        "pm" -> if (h < 12) h += 12
                        "am" -> if (h == 12) h = 0
                    }
                    return h to min
                }
                val old = if (times.size >= 2) hm(times[0]) else 0 to 0
                val new = hm(times.last())
                return EvIntent.EditAlarm(old.first, old.second, new.first, new.second)
            }
        }

        // Notes must be separated from reminders: "delete note" previously hit the
        // reminders store and reported "you have no reminders to delete".
        val noteDelete = Regex(
            "^(?:delete|remove|clear|cancel|erase)\\s+(?:all\\s+|my\\s+|the\\s+)*notes?(?:\\s+(?:about|for|named|called|titled)\\s+(.+))?$"
        ).find(low)
        if (noteDelete != null) {
            return EvIntent.DeleteNote(
                title = noteDelete.groupValues[1].ifBlank { null },
                all = low.contains("all")
            )
        }

        // Calendar / schedule viewing (handles the common "calender" misspelling). Creating
        // an event is left to the classifier so "add calendar event ..." still works.
        val creatingEvent = Regex("^(?:add|create|new|make|schedule|book)\\b").containsMatchIn(low) &&
            !low.startsWith("my schedule")
        if (!creatingEvent && (
                low.contains("calendar") || low.contains("calender") ||
                    low.contains("agenda") || low.contains("my schedule") ||
                    low.contains("schedule for") || low.contains("todays schedule") ||
                    low.contains("what's on my schedule")
                )
        ) {
            return EvIntent.GetCalendarEvents(
                date = when {
                    low.contains("tomorrow") -> "tomorrow"
                    low.contains("today") -> "today"
                    else -> null
                }
            )
        }

        val appMatch = Regex("^(open|launch|start|run)\\s+(.+)$").find(low)
        if (appMatch != null) {
            val rawName = appMatch.groupValues[2].trim()
            val name = rawName.removeSuffix(" app").trim()
            val explicitApp = rawName.length != name.length
            // Only reroute bare targets; an explicit "... app" suffix means the user
            // genuinely wants to launch the app, so keep that as OpenApp.
            if (!explicitApp) {
                when {
                    name == "maps" || name == "google maps" -> return EvIntent.OpenMaps()
                    name == "camera" -> return EvIntent.OpenCamera()
                    // Let the NLU pipeline route settings (and settings sub-panels such
                    // as "open wifi settings") to the dedicated OpenSettings intents.
                    name == "settings" || name.endsWith(" settings") -> return null
                }
            }
            return EvIntent.OpenApp(
                name.replaceFirstChar { it.uppercase(Locale.ROOT) },
                context?.let { resolvePackageName(it, name) }
            )
        }
        val closeMatch = Regex("^close\\s+(.+)$").find(low)
        if (closeMatch != null) return EvIntent.CloseApp(closeMatch.groupValues[1])
        val switchMatch = Regex("^switch\\s+to\\s+(.+)$").find(low)
        if (switchMatch != null) return EvIntent.SwitchApp(switchMatch.groupValues[1])

        return null
    }

    /** Strip wake words and trailing punctuation. */
    fun stripWakePrefixes(text: String): String {
        var r = text.trim()
        var changed = true
        while (changed) {
            changed = false
            for (pattern in WAKE_PREFIXES) {
                val m = pattern.matcher(r)
                if (m.find() && m.start() == 0) {
                    r = r.substring(m.end()).trim()
                    changed = true
                    break
                }
            }
        }
        return r.trimEnd('?', '!', '.').trim()
    }

    /** Normalize a string (public for testing/reuse). */
    fun normalize(text: String): String = Normalizer.normalize(text)

    fun resolvePackageName(context: Context, appNameQuery: String): String? {
        return try {
            val pm = context.packageManager
            val launcherIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }
            val clean = appNameQuery.lowercase(Locale.ROOT).replace(Regex("\\s+"), "")
            var best: String? = null
            for (info in pm.queryIntentActivities(launcherIntent, 0)) {
                val label = info.loadLabel(pm).toString().lowercase(Locale.ROOT)
                    .replace(Regex("\\s+"), "")
                val packageName = info.activityInfo.packageName
                if (label == clean || packageName.lowercase(Locale.ROOT).contains(clean)) {
                    return packageName
                }
                if ((label.contains(clean) || clean.contains(label)) && best == null) {
                    best = packageName
                }
            }
            best
        } catch (_: RuntimeException) {
            null
        }
    }

    // ── Intent construction ───────────────────────────────────────────────

    private fun buildIntent(
        intentName: String,
        entities: Map<String, List<String>>,
        rawQuery: String,
        context: Context?,
        confidence: Double
    ): EvIntent = when {
        // ── Conversation ──
        intentName == "conversation.greeting" -> EvIntent.Conversation(
            PersonaResponder.greeting(context),
            suggestedActions = listOf("What time is it?", "Open WhatsApp", "Set an alarm", "Turn on Bluetooth")
        )
        intentName == "conversation.who_are_you" -> EvIntent.Conversation(
            PersonaResponder.whoAreYou(context),
            suggestedActions = listOf("What can you do?", "Help me", "Set a timer")
        )
        intentName == "conversation.capabilities" -> EvIntent.Conversation(
            "I can open and close apps, set alarms and timers, check time and date, " +
                "control Wi-Fi, Bluetooth, flashlight, volume and brightness, make calls, " +
                "send SMS and WhatsApp, check weather, news, sports, do math, convert units, " +
                "create reminders and notes, control music, and search the web.",
            suggestedActions = listOf("Weather today", "Set alarm 7 AM", "Open Spotify", "Call Mom")
        )
        intentName == "conversation.how_are_you" -> EvIntent.Conversation(
            PersonaResponder.howAreYou(context),
            suggestedActions = listOf("Check time", "Open settings", "Play music")
        )
        intentName == "conversation.thanks" -> EvIntent.Conversation(
            PersonaResponder.thanks(context),
            suggestedActions = emptyList()
        )
        intentName == "conversation.help" -> EvIntent.Help()
        intentName == "conversation.cancel" -> EvIntent.Cancel
        intentName == "conversation.repeat" -> EvIntent.Repeat

        // ── Time / Date ──
        intentName == "system.get_time" -> EvIntent.GetTime
        intentName == "system.get_date" -> EvIntent.GetDate
        intentName == "system.get_day_of_week" -> EvIntent.GetDayOfWeek(
            EntityRecognizer().extract(rawQuery, emptyList(), "system.get_day_of_week")["DATE"]?.firstOrNull()
        )

        // ── Alarms ──
        intentName == "alarm.set" -> buildAlarm(entities, rawQuery)
        intentName == "alarm.delete" -> EvIntent.DeleteAlarm(
            label = entities["CONTACT"]?.firstOrNull(),
            all = rawQuery.contains("all", ignoreCase = true)
        )
        intentName == "alarm.edit" -> EvIntent.EditAlarm(
            oldHour = 0, oldMinute = 0, newHour = 0, newMinute = 0,
            label = entities["CONTACT"]?.firstOrNull()
        )
        intentName == "alarm.snooze" -> EvIntent.SnoozeAlarm(
            minutes = entities["NUMBER"]?.firstOrNull()?.toIntOrNull() ?: 9
        )
        intentName == "alarm.check" -> EvIntent.CheckAlarm

        // ── Timers ──
        intentName == "timer.set" -> {
            val secs = entityRecognizer.firstDurationSeconds(entities) ?: 60
            EvIntent.SetTimer(durationSeconds = secs, label = "EV Timer")
        }
        intentName == "timer.cancel" -> EvIntent.CancelTimer(
            all = rawQuery.contains("all", ignoreCase = true)
        )
        intentName == "timer.pause" -> EvIntent.PauseTimer
        intentName == "timer.resume" -> EvIntent.ResumeTimer
        intentName == "timer.check" -> EvIntent.CheckTimer

        // ── Connectivity ──
        intentName == "connectivity.wifi_on" -> EvIntent.ToggleConnectivity(ConnectivityType.WIFI, true)
        intentName == "connectivity.wifi_off" -> EvIntent.ToggleConnectivity(ConnectivityType.WIFI, false)
        intentName == "connectivity.bluetooth_on" -> EvIntent.ToggleConnectivity(ConnectivityType.BLUETOOTH, true)
        intentName == "connectivity.bluetooth_off" -> EvIntent.ToggleConnectivity(ConnectivityType.BLUETOOTH, false)
        intentName == "connectivity.hotspot_on" -> EvIntent.ToggleConnectivity(ConnectivityType.HOTSPOT, true)
        intentName == "connectivity.hotspot_off" -> EvIntent.ToggleConnectivity(ConnectivityType.HOTSPOT, false)
        intentName == "connectivity.mobile_data_on" -> EvIntent.ToggleConnectivity(ConnectivityType.MOBILE_DATA, true)
        intentName == "connectivity.mobile_data_off" -> EvIntent.ToggleConnectivity(ConnectivityType.MOBILE_DATA, false)
        intentName == "connectivity.status" -> {
            val type = if (rawQuery.contains("bluetooth", ignoreCase = true)) ConnectivityType.BLUETOOTH
            else if (rawQuery.contains("hotspot", ignoreCase = true)) ConnectivityType.HOTSPOT
            else ConnectivityType.WIFI
            EvIntent.GetConnectivityStatus(type)
        }
        intentName == "connectivity.network_info" -> EvIntent.GetNetworkInfo

        // ── Settings ──
        intentName == "settings.open" -> EvIntent.OpenSettings(SettingType.GENERAL, "Settings")
        intentName == "settings.wifi" -> EvIntent.OpenSettings(SettingType.WIFI, "Wi-Fi Settings")
        intentName == "settings.bluetooth" -> EvIntent.OpenSettings(SettingType.BLUETOOTH, "Bluetooth Settings")
        intentName == "settings.display" -> EvIntent.OpenSettings(SettingType.DISPLAY, "Display Settings")
        intentName == "settings.sound" -> EvIntent.OpenSettings(SettingType.SOUND, "Sound Settings")
        intentName == "settings.battery" -> EvIntent.OpenSettings(SettingType.BATTERY, "Battery Settings")
        intentName == "settings.apps" -> EvIntent.OpenSettings(SettingType.APPS, "App Settings")

        // ── Device ──
        intentName == "device.set_brightness" -> {
            val pct = entities["PERCENT"]?.firstOrNull()?.toIntOrNull()
                ?: entities["NUMBER"]?.firstOrNull()?.toIntOrNull() ?: 50
            EvIntent.SetBrightness(level = coerceIn(pct, 0, 100))
        }
        intentName == "device.set_volume" -> {
            val lowVol = rawQuery.lowercase(Locale.ROOT)
            val stream = when {
                lowVol.contains("media") || lowVol.contains("music") -> VolumeStream.MEDIA
                lowVol.contains("ring") -> VolumeStream.RING
                lowVol.contains("alarm") -> VolumeStream.ALARM
                lowVol.contains("notification") -> VolumeStream.NOTIFICATION
                lowVol.contains("call") -> VolumeStream.CALL
                else -> VolumeStream.MEDIA
            }
            val isMute = lowVol.contains("mute") && !lowVol.contains("unmute")
            val isUnmute = lowVol.contains("unmute")
            val relative = !isMute && !isUnmute &&
                (lowVol.contains("up") || lowVol.contains("increase") ||
                    lowVol.contains("down") || lowVol.contains("decrease"))
            val pct = entities["PERCENT"]?.firstOrNull()?.toIntOrNull()
                ?: entities["NUMBER"]?.firstOrNull()?.toIntOrNull()
                ?: 50
            val level = when {
                isMute -> 0
                isUnmute -> 50
                lowVol.contains("max") -> 100
                else -> coerceIn(pct, 0, 100)
            }
            EvIntent.SetVolume(stream = stream, level = level, relative = relative)
        }
        intentName == "device.flashlight" -> {
            val enabled = when {
                rawQuery.contains("off", ignoreCase = true) -> false
                rawQuery.contains("on", ignoreCase = true) -> true
                else -> null
            }
            EvIntent.ToggleFlashlight(enabled)
        }
        intentName == "device.battery_status" -> EvIntent.GetBatteryStatus
        intentName == "device.storage_status" -> EvIntent.GetStorageStatus
        intentName == "device.airplane_mode" -> {
            val enabled = !rawQuery.contains("off", ignoreCase = true) && !rawQuery.contains("disable", ignoreCase = true)
            EvIntent.SetAirplaneMode(enabled)
        }
        intentName == "device.do_not_disturb" -> {
            val enabled = !rawQuery.contains("off", ignoreCase = true) && !rawQuery.contains("disable", ignoreCase = true)
            val mins = entities["NUMBER"]?.firstOrNull()?.toIntOrNull()
            EvIntent.SetDoNotDisturb(enabled, mins)
        }
        intentName == "device.lock_screen" -> EvIntent.LockScreen
        intentName == "device.take_screenshot" -> EvIntent.TakeScreenshot
        intentName == "device.open_camera" -> EvIntent.OpenCamera()

        // ── Apps ──
        intentName == "app.open" -> {
            val appName = entities["APP"]?.firstOrNull()
                ?: extractAppNameFromQuery(rawQuery)
            EvIntent.OpenApp(appName ?: rawQuery)
        }
        intentName == "app.close" -> {
            val appName = entities["APP"]?.firstOrNull() ?: extractAppNameFromQuery(rawQuery)
            EvIntent.CloseApp(appName ?: rawQuery)
        }
        intentName == "app.switch" -> {
            val appName = entities["APP"]?.firstOrNull() ?: extractAppNameFromQuery(rawQuery)
            EvIntent.SwitchApp(appName ?: rawQuery)
        }

        // ── Communication ──
        intentName == "comm.call" -> {
            val contact = entities["CONTACT"]?.firstOrNull()
            EvIntent.MakeCall(target = contact ?: rawQuery, displayName = contact)
        }
        intentName == "comm.send_sms" -> {
            val contact = entities["CONTACT"]?.firstOrNull()
            val message = extractMessageAfterKeyword(rawQuery, setOf("sms", "text", "message"))
            EvIntent.SendSms(recipient = contact ?: "", message = message)
        }
        intentName == "comm.send_whatsapp" -> {
            val contact = entities["CONTACT"]?.firstOrNull()
            val message = extractMessageAfterKeyword(rawQuery, setOf("whatsapp", "message", "chat"))
            EvIntent.SendWhatsAppMessage(recipient = contact, message = message)
        }
        intentName == "comm.send_email" -> {
            EvIntent.SendEmail(
                recipient = entities["CONTACT"]?.firstOrNull(),
                subject = null,
                body = extractMessageAfterKeyword(rawQuery, setOf("email", "mail"))
            )
        }
        intentName == "comm.read_messages" -> EvIntent.ReadMessages(
            limit = entities["NUMBER"]?.firstOrNull()?.toIntOrNull() ?: 5,
            unreadOnly = rawQuery.contains("unread", ignoreCase = true)
        )

        // ── Web Search ──
        intentName == "web.search" -> {
            val query = stripSearchPrefixes(rawQuery)
            EvIntent.SearchWeb(query)
        }

        // ── Information ──
        intentName == "info.weather" -> {
            val location = entities["LOCATION"]?.firstOrNull()
            val forecast = rawQuery.contains("tomorrow", ignoreCase = true) ||
                rawQuery.contains("forecast", ignoreCase = true)
            EvIntent.GetWeather(location = location, forecast = forecast)
        }
        intentName == "info.news" -> EvIntent.GetNews(
            category = null,
            limit = entities["NUMBER"]?.firstOrNull()?.toIntOrNull() ?: 5
        )
        intentName == "info.sports" -> EvIntent.GetSportsScore(
            team = extractTrailingSubject(rawQuery),
            league = null
        )
        intentName == "info.stock" -> EvIntent.GetStockPrice(
            symbol = extractTrailingSubject(rawQuery).uppercase(Locale.ROOT)
        )
        intentName == "info.definition" -> EvIntent.GetDefinition(
            word = extractDefinitionWord(rawQuery)
        )
        intentName == "info.translate" -> {
            val m = Regex("""translate\s+(.+?)\s+(?:to|into|in)\s+([a-zA-Z]+)""", RegexOption.IGNORE_CASE)
                .find(rawQuery.trim())
            EvIntent.Translate(
                text = m?.groupValues?.get(1)?.trim()
                    ?: extractAfterKeyword(rawQuery, setOf("translate", "translation")),
                targetLanguage = m?.groupValues?.get(2)?.trim()?.lowercase(Locale.ROOT),
                sourceLanguage = null
            )
        }

        // ── Media ──
        intentName == "media.play" -> {
            val query = entities["MUSIC_QUERY"]?.firstOrNull()
            EvIntent.PlayMusic(query = query, resume = rawQuery.contains("resume", ignoreCase = true))
        }
        intentName == "media.pause" -> EvIntent.PauseMusic
        intentName == "media.next" -> EvIntent.NextTrack
        intentName == "media.previous" -> EvIntent.PreviousTrack
        intentName == "media.stop" -> EvIntent.StopMusic
        intentName == "media.set_volume" -> {
            val pct = entities["PERCENT"]?.firstOrNull()?.toIntOrNull()
                ?: entities["NUMBER"]?.firstOrNull()?.toIntOrNull() ?: 50
            EvIntent.SetMusicVolume(level = coerceIn(pct, 0, 100))
        }

        // ── Productivity ──
        intentName == "productivity.create_reminder" -> {
            val title = entities["REMINDER_TITLE"]?.firstOrNull() ?: rawQuery
            val timeStr = entities["TIME"]?.firstOrNull()
            val dateStr = entities["DATE"]?.firstOrNull()
            EvIntent.CreateReminder(title = title, dueTime = timeStr, dueDate = dateStr)
        }
        intentName == "productivity.list_reminders" -> EvIntent.ListReminders(
            pending = !rawQuery.contains("all", ignoreCase = true),
            all = rawQuery.contains("all", ignoreCase = true)
        )
        intentName == "productivity.delete_reminder" -> EvIntent.DeleteReminder(
            title = entities["REMINDER_TITLE"]?.firstOrNull(),
            all = rawQuery.contains("all", ignoreCase = true)
        )
        intentName == "productivity.create_note" -> EvIntent.CreateNote(
            title = null,
            content = extractAfterKeyword(rawQuery, setOf("note", "notes", "write", "save", "memo"))
        )
        intentName == "productivity.list_notes" -> EvIntent.ListNotes
        intentName == "productivity.search_notes" -> EvIntent.SearchNotes(
            query = extractAfterKeyword(rawQuery, setOf("search", "find"))
        )
        intentName == "productivity.delete_note" -> EvIntent.DeleteNote(
            title = extractAfterKeyword(rawQuery, setOf("note", "notes")).ifBlank { null },
            all = rawQuery.contains("all", ignoreCase = true)
        )
        intentName == "productivity.create_calendar_event" -> EvIntent.AddCalendarEvent(
            title = extractAfterKeyword(rawQuery, setOf("event", "meeting", "appointment", "calendar")),
            startTime = entities["TIME"]?.firstOrNull(),
            endTime = null,
            date = entities["DATE"]?.firstOrNull()
        )
        intentName == "productivity.get_calendar_events" -> EvIntent.GetCalendarEvents(
            date = entities["DATE"]?.firstOrNull(),
            limit = entities["NUMBER"]?.firstOrNull()?.toIntOrNull() ?: 5
        )

        // ── Calculation ──
        intentName == "calc.calculate" -> {
            val expression = stripCalcPrefixes(rawQuery)
            val result = calculate(expression)
            EvIntent.Calculate(expression = expression, result = result)
        }
        intentName == "calc.convert_units" -> {
            val value = entities["CONVERT_VALUE"]?.firstOrNull()?.toDoubleOrNull()
                ?: entities["NUMBER"]?.firstOrNull()?.toDoubleOrNull() ?: 0.0
            EvIntent.ConvertUnits(
                value = value,
                fromUnit = entities["FROM_UNIT"]?.firstOrNull() ?: "",
                toUnit = entities["TO_UNIT"]?.firstOrNull() ?: "",
                result = null
            )
        }

        // ── Navigation ──
        intentName == "nav.open_maps" -> EvIntent.OpenMaps(
            destination = entities["LOCATION"]?.firstOrNull()
        )
        intentName == "nav.get_directions" -> EvIntent.GetDirections(
            origin = null,
            destination = entities["LOCATION"]?.firstOrNull() ?: rawQuery
        )

        // ── System ──
        intentName == "system.shutdown" -> EvIntent.Shutdown(confirm = false)
        intentName == "system.restart" -> EvIntent.Restart

        // ── Fallback ──
        else -> EvIntent.Unknown(
            rawQuery = rawQuery,
            confidence = confidence,
            suggestions = buildSuggestions(intentName, confidence, rawQuery)
        )
    }

    // ── Alarm builder ──────────────────────────────────────────────────────

    private fun buildAlarm(entities: Map<String, List<String>>, rawQuery: String): EvIntent.SetAlarm {
        // Prefer the flexible spoken-time parser (handles "7 30 pm", "quarter past
        // seven", "7 in the morning"); fall back to the TIME entity's "HH:MM" shape.
        val spoken = parseSpokenTime(rawQuery.lowercase(Locale.ROOT))
        if (spoken != null && spoken.first in 0..23 && spoken.second in 0..59) {
            return EvIntent.SetAlarm(hour = spoken.first, minute = spoken.second)
        }
        val timeList = entities["TIME"]
        var hour = 7
        var minute = 0
        if (timeList != null && timeList.isNotEmpty()) {
            val timeStr = timeList.first()
            val parts = timeStr.split(":")
            if (parts.size >= 2) {
                hour = parts[0].toIntOrNull() ?: 7
                minute = parts[1].toIntOrNull() ?: 0
            } else {
                hour = timeStr.toIntOrNull() ?: 7
            }
        }
        // AM/PM adjustment from raw query
        val low = rawQuery.lowercase(Locale.ROOT)
        if (low.contains("pm") && hour < 12) hour += 12
        if (low.contains("am") && hour == 12) hour = 0
        return EvIntent.SetAlarm(hour = hour, minute = minute)
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Non-"set" alarm actions. The flexible time-based alarm rule must never fire
     * for these, otherwise "delete alarm", "snooze", "edit alarm from 7 to 8" would
     * be misread as "set an alarm at 7".
     */
    private val LOW_PRIORITY_ALARM_VERBS = Regex(
        """\b(delete|remove|cancel|clear|disable|snooze|check|edit|change|move|reschedule|stop|dismiss|what|when|list|how many)\b"""
    )

    /** Navigation phrases that clearly ask for directions to a destination. */
    private val NAV_PHRASES = listOf(
        "locate me to ", "locate me at ", "take me to ", "navigate to ", "navigation to ",
        "directions to ", "direction to ", "route to ", "get me directions to ",
        "how do i get to ", "how to get to ", "how do i go to ", "how to go to ",
        "show me the way to ", "find route to ", "get directions to "
    )

    /**
     * Extracts a navigation destination from phrases like "locate me to satara",
     * "take me to pune", "directions to the airport". Returns null when the query
     * does not clearly name a destination, so it falls through to other handlers.
     */
    private fun navDestination(low: String): String? {
        for (phrase in NAV_PHRASES) {
            val idx = low.indexOf(phrase)
            if (idx < 0) continue
            var dest = low.substring(idx + phrase.length)
            // Trim trailing filler / politeness words.
            dest = dest.replace(
                Regex("""\s+(please|now|today|tomorrow|asap|for me)[.!?]*$"""), ""
            ).trim().trimEnd('?', '!', '.').trim()
            if (dest.isNotBlank() && dest !in setOf("it", "there", "somewhere", "the place")) {
                return dest
            }
        }
        return null
    }

    /**
     * Extracts a nearby-search subject: "hospital near me" → "hospital",
     * "find an atm near me" → "atm", "nearby hotels" → "hotels".
     * Returns null when the query is not a nearby search.
     */
    private fun nearbyQuery(low: String): String? {
        // "<subject> near me / by / here / my location"
        Regex(
            """^(?:find\s+(?:an?\s+)?|search\s+(?:for\s+(?:an?\s+)?)?|is\s+there\s+(?:an?\s+)?|show\s+me\s+|where\s+is\s+(?:an?\s+)?|locate\s+)?(.+?)\s+near\s+(?:me|by|here|my\s+location|around\s+me)[.!?]*$"""
        ).find(low)?.let { m ->
            val subject = m.groupValues[1].trim()
            if (subject.isNotBlank() && subject !in setOf("what", "which", "where", "are", "is")) {
                return subject
            }
        }
        // "nearby <subject>" / "closest <subject>" / "nearest <subject>"
        Regex("""^(?:the\s+)?(?:nearby|closest|nearest)\s+(.+?)[.!?]*$""").find(low)?.let { m ->
            val subject = m.groupValues[1].trim()
            if (subject.isNotBlank()) return subject
        }
        return null
    }

    /**
     * Maps camera phrases to an [EvIntent.OpenCamera]: open front/back camera,
     * take a photo/selfie, or start a video recording. Returns null otherwise.
     */
    private fun cameraIntent(low: String): EvIntent? {
        // "open camera app" explicitly asks for the application, so let the
        // app-matching path handle it instead of the capture intents.
        if (low.contains("camera app")) return null
        val isCamera = low.contains("camera")
        val isPhoto = Regex(
            """\b(?:take|click|snap|capture|shoot)\b.*\b(?:photo|picture|pic|selfie)\b"""
        ).containsMatchIn(low) || low.contains("selfie")
        val isVideo = Regex(
            """\b(?:record|start|begin)\b.*\b(?:video|recording)\b"""
        ).containsMatchIn(low) && !isPhoto
        if (!isCamera && !isPhoto && !isVideo) return null
        val front = low.contains("front") || low.contains("selfie")
        val video = isVideo && !isPhoto
        return EvIntent.OpenCamera(
            facing = if (front) "front" else "back",
            mode = if (video) "video" else "still"
        )
    }

    /**
     * A small set of day-part words used to infer am/pm when the user says
     * "7 in the morning" or "8 in the evening" without an explicit am/pm.
     */
    private val MORNING_WORDS = setOf("morning", "am", "a.m.", "subah", "savera")
    private val EVENING_WORDS = setOf("evening", "night", "tonight", "pm", "p.m.", "raat")

    /**
     * Parses a flexible spoken time into 24-hour (hour, minute).
     *
     * Handles the shapes people actually say: "7", "7:30", "07:05", "7 30",
     * "7:30 pm", "7 in the morning", "quarter past seven", "half past 7",
     * "twelve noon", "midnight", "19:00". Returns null when no time is found.
     */
    internal fun parseSpokenTime(low: String): Pair<Int, Int>? {
        // Word numbers so "quarter past seven" / "half past 7" work.
        val words = mapOf(
            "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6,
            "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10, "eleven" to 11,
            "twelve" to 12, "thirteen" to 13, "fourteen" to 14, "fifteen" to 15,
            "sixteen" to 16, "seventeen" to 17, "eighteen" to 18, "nineteen" to 19,
            "twenty" to 20, "thirty" to 30, "forty" to 40, "fifty" to 50
        )
        fun wordNum(token: String): Int? {
            val t = token.trim().trimEnd('.', ',')
            if (t.isEmpty()) return null
            t.toIntOrNull()?.let { if (it in 0..59) return it }
            return words[t]
        }

        if (low.isBlank()) return null
        if (Regex("""\bmidnight\b""").containsMatchIn(low)) return 0 to 0
        if (Regex("""\b(?:12\s*)?noon\b|\bmidday\b""").containsMatchIn(low)) return 12 to 0

        // explicit am/pm / day-part markers
        val wantsPm = EVENING_WORDS.any { low.contains(it) }
        val wantsAm = MORNING_WORDS.any { low.contains(it) }

        // 1) Numeric clock: "7:30", "07:05", "19:00", "7.30"
        Regex("""\b(\d{1,2})[:.](\d{2})\b""").find(low)?.let { m ->
            val h = m.groupValues[1].toInt(); val min = m.groupValues[2].toIntOrNull() ?: 0
            if (h in 0..23 && min in 0..59) return applyAmPm(h, min, wantsPm, wantsAm)
        }

        // 2) "quarter past|half past <hour>"
        Regex("""\b(quarter|half)\s+(?:past|after)\s+(\w+)\b""").find(low)?.let { m ->
            val h = wordNum(m.groupValues[2])
            if (h != null) {
                val min = if (m.groupValues[1].startsWith("quarter")) 15 else 30
                if (h in 0..23) return applyAmPm(h, min, wantsPm, wantsAm)
            }
        }

        // 3) Space or colon separated "7 30", "7 thirty"
        Regex("""\b(\d{1,2})[\s:]+(\d{1,2}|\w+)\b""").find(low)?.let { m ->
            val h = m.groupValues[1].toIntOrNull()
            val min = wordNum(m.groupValues[2])
            if (h != null && min != null && h in 0..23 && min in 0..59) {
                return applyAmPm(h, min, wantsPm, wantsAm)
            }
        }

        // 4) Bare hour: "set alarm for 7", "wake me at 6"
        Regex("""\b(\d{1,2})\b""").find(low)?.let { m ->
            val h = m.groupValues[1].toIntOrNull()
            if (h != null && h in 0..23) return applyAmPm(h, 0, wantsPm, wantsAm)
        }

        // 5) Word hour: "set alarm for seven"
        for (token in low.split(Regex("""\s+"""))) {
            wordNum(token)?.let { h -> if (h in 1..23) return applyAmPm(h, 0, wantsPm, wantsAm) }
        }
        return null
    }

    /** Converts a 12-hour reading to 24-hour using explicit or day-part am/pm. */
    private fun applyAmPm(hour: Int, minute: Int, wantsPm: Boolean, wantsAm: Boolean): Pair<Int, Int> {
        var h = hour
        when {
            wantsPm && h < 12 -> h += 12
            wantsAm && h == 12 -> h = 0
            // A bare 1-11 with no marker: assume the next occurrence is fine as-is,
            // but treat a plain 12 as noon (12:00) rather than midnight.
        }
        return h to minute
    }

    /**
     * True when a low-confidence query is really a request for factual knowledge
     * ("who is the president", "president of usa", "capital of france", "how tall is
     * the eiffel tower"). Such queries have no on-device action, so they should be
     * answered from the web instead of the canned "I didn't understand" path.
     */
    private fun looksLikeKnowledgeQuestion(query: String): Boolean {
        val low = query.lowercase(Locale.ROOT).trim().trimEnd('?', '!', '.')
        if (low.isBlank()) return false

        // Imperative device commands are never knowledge questions.
        val firstWord = low.substringBefore(' ', low)
        if (firstWord in COMMAND_VERBS) return false

        // Explicit question openers.
        val openers = listOf(
            "who ", "what ", "when ", "where ", "why ", "how ", "which ", "whose ",
            "tell me about ", "explain ", "do you know ", "can you tell me ",
            "i want to know ", "what's ", "whats "
        )
        if (openers.any { low == it.trimEnd() || low.startsWith(it) }) return true

        // Topic phrases: "president of usa", "capital of france", "founder of microsoft".
        if (KNOWLEDGE_TOPIC.containsMatchIn(low)) return true

        // Deliberately no catch-all here. A multi-word phrase that has no question
        // opener and no known "… of …" topic shape ("florb the gunk") is treated as
        // gibberish and stays Unknown rather than firing a pointless web lookup.
        return false
    }

    // Verbs that begin real device commands; a query starting with one of these is an
    // action to perform, not a fact to look up.
    private val COMMAND_VERBS = setOf(
        "open", "close", "switch", "start", "stop", "launch", "run", "play", "pause",
        "resume", "next", "previous", "turn", "set", "adjust", "increase", "decrease",
        "raise", "lower", "mute", "unmute", "enable", "disable", "activate", "deactivate",
        "call", "dial", "phone", "ring", "send", "text", "message", "email", "delete",
        "remove", "cancel", "clear", "create", "add", "make", "take", "check", "show",
        "list", "read", "search", "google", "look", "find", "navigate", "go", "shut",
        "reboot", "restart", "define", "translate", "convert", "calculate", "compute",
        "solve", "remind", "schedule", "lock", "capture", "install", "save", "write",
        "record", "volume", "brightness", "wifi", "bluetooth", "hotspot", "airplane"
    )

    // "… of …" topic shape used for noun-style knowledge queries.
    private val KNOWLEDGE_TOPIC = Regex(
        "\\b(?:president|prime minister|king|queen|capital|founder|inventor|author|" +
            "population|currency|language|height|weight|distance|meaning|definition|" +
            "symbol|unit|color|colour|leader|ceo|head|owner|winner|price)\\b\\s+of\\s+\\S+"
    )

    private fun buildSuggestions(intent: String, confidence: Double, query: String): List<String> {
        val suggestions = mutableListOf<String>()
        if (confidence < 0.3) {
            suggestions.addAll(listOf("Try saying 'open WhatsApp'", "Try 'set alarm 7 AM'", "Try 'what time is it?'"))
        }
        if (query.contains("weather", ignoreCase = true)) suggestions.add("Try 'weather in London'")
        if (query.contains("time", ignoreCase = true)) suggestions.add("Try 'what time is it'")
        return suggestions.distinct().take(3)
    }

    private fun extractAppNameFromQuery(rawQuery: String): String? {
        val low = rawQuery.lowercase(Locale.ROOT)
        val verbs = listOf("open", "launch", "start", "run", "show", "go to", "take me to", "close", "quit", "exit", "switch to")
        var rest = low
        for (v in verbs) rest = rest.removePrefix(v).trim()
        for (w in listOf("app", "application", "software", "the", "a", "an")) {
            rest = rest.removePrefix(w).trim()
        }
        return if (rest.isNotBlank()) rest.capitalize(Locale.ROOT) else null
    }

    private fun stripSearchPrefixes(rawQuery: String): String {
        var q = rawQuery.lowercase(Locale.ROOT)
        for (p in listOf("search", "search for", "google", "look up", "search google for", "search the web", "find", "browse")) {
            q = q.removePrefix(p).trim()
        }
        return q.trim()
    }

    private fun stripCalcPrefixes(rawQuery: String): String {
        var q = rawQuery.lowercase(Locale.ROOT)
        for (p in listOf("calculate", "compute", "solve", "what is", "how much is", "math")) {
            q = q.removePrefix(p).trim()
        }
        return q.trim()
    }

    /** Pull the target word out of "what is the meaning of X" / "define X". */
    private fun extractDefinitionWord(rawQuery: String): String {
        var q = rawQuery.lowercase(Locale.ROOT).trim().trimEnd('?', '!', '.')
        val prefixes = listOf(
            "what is the meaning of", "what's the meaning of", "whats the meaning of",
            "what is the definition of", "what's the definition of", "whats the definition of",
            "the meaning of", "the definition of",
            "definition of", "meaning of", "definition", "meaning",
            "what does", "what is", "what's", "whats", "define", "means"
        )
        // Strip repeatedly so "what is meaning of X" and "what is the meaning of X"
        // both collapse to "X".
        var changed = true
        while (changed) {
            changed = false
            for (p in prefixes) {
                if (q.startsWith(p)) {
                    q = q.removePrefix(p).trim()
                    changed = true
                    break
                }
            }
        }
        return q.trim()
    }

    /** Extracts the subject of "stock price of apple" / "score of the lakers" -> "apple" / "lakers". */
    private fun extractTrailingSubject(rawQuery: String): String {
        var q = rawQuery.lowercase(Locale.ROOT).trim().trimEnd('?', '!', '.')
        val leading = listOf(
            "what is the", "what's the", "whats the", "what is", "what's", "whats",
            "show me the", "show me", "tell me the", "tell me", "get me the", "get me",
            "give me the", "give me", "check the", "check", "the"
        )
        var changed = true
        while (changed) {
            changed = false
            for (p in leading) {
                if (q.startsWith(p)) { q = q.removePrefix(p).trim(); changed = true; break }
            }
        }
        val markers = listOf(" of ", " for ", " about ", " in ")
        var bestIdx = -1
        var bestLen = 0
        for (m in markers) {
            val i = q.lastIndexOf(m)
            if (i > bestIdx) { bestIdx = i; bestLen = m.length }
        }
        if (bestIdx >= 0) {
            q = q.substring(bestIdx + bestLen).trim()
        } else {
            for (p in listOf("stock price", "stock quote", "share price", "stock", "shares", "price", "quote", "scores", "score")) {
                if (q.startsWith(p)) q = q.removePrefix(p).trim()
            }
        }
        q = q.removePrefix("the ").trim()
        return q.trim()
    }

    private fun extractMessageAfterKeyword(rawQuery: String, keywords: Set<String>): String {
        var rest = rawQuery.lowercase(Locale.ROOT)
        for (k in keywords) {
            val idx = rest.indexOf(k)
            if (idx >= 0) {
                rest = rest.substring(idx + k.length).trim()
                break
            }
        }
        return if (rest.isNotBlank()) rest.capitalize(Locale.ROOT) else ""
    }

    private fun extractAfterKeyword(rawQuery: String, keywords: Set<String>): String {
        var rest = rawQuery.lowercase(Locale.ROOT)
        for (k in keywords) {
            val idx = rest.indexOf(k)
            if (idx >= 0) {
                rest = rest.substring(idx + k.length).trim()
                break
            }
        }
        return rest.trim()
    }

    /** Very small arithmetic evaluator for binary expressions. */
    private fun calculate(expression: String): Double {
        val cleaned = expression.trim()
        val operators = listOf("+", "-", "*", "x", "×", "÷", "/", "plus", "minus", "times", "divided by", "multiplied by")
        for (op in operators) {
            val parts = cleaned.split(op, limit = 2)
            if (parts.size == 2) {
                val a = parts[0].trim().toDoubleOrNull() ?: return 0.0
                val b = parts[1].trim().toDoubleOrNull() ?: return 0.0
                return when (op) {
                    "+", "plus" -> a + b
                    "-", "minus" -> a - b
                    "*", "x", "×", "times", "multiplied by" -> a * b
                    "/", "÷", "divided by" -> if (b != 0.0) a / b else Double.NaN
                    else -> 0.0
                }
            }
        }
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    private fun coerceIn(value: Int, min: Int, max: Int): Int = maxOf(min, minOf(max, value))
}