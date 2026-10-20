package com.ev.ai.assistant.nlu.nlp

import com.ev.ai.assistant.nlu.EvIntent
import java.util.Locale
import java.util.regex.Pattern

/**
 * Fast, dependency-free intent classifier.
 *
 * Strategy (all sub-millisecond for short voice queries):
 *   1. Exact phrase matching (highest weight)
 *   2. Keyword coverage scoring per intent
 *   3. Regex pattern matching for structured entities (time, number, etc.)
 *   4. Fuzzy matching (Jaro-Winkler) against known phrases as fallback
 *   5. Confidence scoring; if max confidence < threshold, return "unknown"
 *
 * No TensorFlow Lite / ONNX runtime — keeps app startup < 100ms and inference
 * under 1ms. Patterns are defined here so they are easy to extend.
 */
class IntentClassifier {

    private val CONFIDENCE_THRESHOLD = 0.42
    private val FUZZY_THRESHOLD = 0.78

    // ── Intent pattern definitions ─────────────────────────────────────────
    // Each intent maps to:
    //   keywords   - tokens that strongly indicate this intent
    //   phrases    - exact/fuzzy phrases to match
    //   regexes    - compiled regex patterns
    //   required   - tokens that MUST be present (boosts confidence)
    //   excludes   - tokens that suggest a different intent (suppresses)

    private data class IntentDef(
        val keywords: Set<String> = emptySet(),
        val phrases: List<String> = emptyList(),
        val regexes: List<Pattern> = emptyList(),
        val required: Set<String> = emptySet(),
        val excludes: Set<String> = emptySet(),
        val weight: Double = 1.0
    )

    private val intents = mapOf(
        // ── Conversation / Greetings ──
        "conversation.greeting" to IntentDef(
            keywords = setOf("hello", "hi", "hey", "goodmorning", "goodafternoon", "goodevening", "goodnight"),
            phrases = listOf("hi", "hello", "hey", "good morning", "good afternoon", "good evening", "good night", "hey ev", "hello ev"),
            weight = 1.2
        ),
        "conversation.who_are_you" to IntentDef(
            keywords = setOf("who", "what", "name", "ev", "jarvis", "assistant", "are", "you"),
            phrases = listOf("who are you", "what are you", "your name", "are you ev", "are you jarvis", "who r u", "whats your name"),
            // Require a self-referential token: without this, a knowledge question such
            // as "who is the president" would score on the bare keyword "who" and the
            // max-normalised confidence would make it look certain.
            required = setOf("you", "name"),
            excludes = setOf("time", "date", "weather", "alarm", "timer", "call", "open"),
            weight = 1.1
        ),
        "conversation.capabilities" to IntentDef(
            keywords = setOf("can", "do", "help", "command", "capability", "feature"),
            phrases = listOf("what can you do", "what do you do", "help me", "commands", "capabilities", "what are your features"),
            weight = 1.1
        ),
        "conversation.how_are_you" to IntentDef(
            keywords = setOf("how", "are", "you", "going", "doing"),
            phrases = listOf("how are you", "hows it going", "you doing", "how is it going"),
            weight = 1.1
        ),
        "conversation.thanks" to IntentDef(
            keywords = setOf("thank", "thanks", "cheers", "appreciate"),
            phrases = listOf("thanks", "thank you", "thank you so much", "thanks ev", "thank you ev", "cheers"),
            weight = 1.1
        ),
        "conversation.help" to IntentDef(
            keywords = setOf("help", "support", "assist", "guide"),
            phrases = listOf("help", "help me", "support", "assist me", "guide me"),
            weight = 1.0
        ),
        "conversation.cancel" to IntentDef(
            keywords = setOf("cancel", "stop", "abort", "nevermind", "never mind"),
            phrases = listOf("cancel", "stop", "abort", "nevermind", "never mind", "forget it"),
            weight = 1.1
        ),
        "conversation.repeat" to IntentDef(
            keywords = setOf("repeat", "again", "say again", "once more"),
            phrases = listOf("repeat", "say again", "again", "once more", "repeat that"),
            weight = 1.1
        ),

        // ── Time & Date ──
        "system.get_time" to IntentDef(
            keywords = setOf("time", "clock", "current", "now"),
            phrases = listOf("what time", "current time", "tell me the time", "time now", "whats the time", "what is the time", "the time", "check time", "time"),
            excludes = setOf("set", "alarm", "timer", "reminder"),
            required = setOf("time", "clock"),
            weight = 1.2
        ),
        "system.get_date" to IntentDef(
            keywords = setOf("date", "day", "today", "tomorrow", "current", "what"),
            phrases = listOf("todays date", "what date", "current date", "what day", "day today", "what is today", "today is", "date today", "whats today", "date"),
            excludes = setOf("set", "alarm", "timer", "reminder"),
            required = setOf("date", "day", "today", "tomorrow"),
            weight = 1.2
        ),
        "system.get_day_of_week" to IntentDef(
            keywords = setOf("day", "week", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"),
            phrases = listOf("what day is it", "what day of the week", "what day", "which day"),
            weight = 1.1
        ),

        // ── Alarms ──
        "alarm.set" to IntentDef(
            keywords = setOf("set", "alarm", "wake", "wakeup", "wake me", "alarm at", "alarm for"),
            phrases = listOf("set alarm", "set an alarm", "alarm at", "alarm for", "wake me up at", "wake me at", "set alarm for", "set alarm at"),
            required = setOf("alarm"),
            weight = 1.3
        ),
        "alarm.delete" to IntentDef(
            keywords = setOf("delete", "remove", "cancel", "alarm", "clear"),
            phrases = listOf("delete alarm", "remove alarm", "cancel alarm", "clear alarm", "delete all alarms"),
            required = setOf("alarm"),
            weight = 1.2
        ),
        "alarm.edit" to IntentDef(
            keywords = setOf("edit", "change", "move", "reschedule", "alarm"),
            phrases = listOf("edit alarm", "change alarm", "move alarm", "reschedule alarm"),
            required = setOf("alarm"),
            weight = 1.1
        ),
        "alarm.snooze" to IntentDef(
            keywords = setOf("snooze", "alarm", "later", "minutes"),
            phrases = listOf("snooze", "snooze alarm", "alarm snooze"),
            required = setOf("snooze"),
            weight = 1.1
        ),
        "alarm.check" to IntentDef(
            keywords = setOf("check", "alarm", "alarms", "list", "show", "my"),
            phrases = listOf("check alarm", "check alarms", "my alarms", "list alarms", "show alarms"),
            required = setOf("alarm", "alarm"),
            weight = 1.1
        ),

        // ── Timers ──
        "timer.set" to IntentDef(
            keywords = setOf("set", "timer", "countdown", "minute", "second", "hour", "for"),
            phrases = listOf("set timer", "set a timer", "timer for", "countdown", "minute timer", "second timer", "hour timer", "10 minute timer"),
            required = setOf("timer", "timer"),
            excludes = setOf("alarm", "alarm"),
            weight = 1.3
        ),
        "timer.cancel" to IntentDef(
            keywords = setOf("cancel", "stop", "timer", "clear"),
            phrases = listOf("cancel timer", "stop timer", "clear timer", "cancel all timers"),
            required = setOf("timer"),
            weight = 1.2
        ),
        "timer.pause" to IntentDef(
            keywords = setOf("pause", "timer"),
            phrases = listOf("pause timer", "pause the timer"),
            required = setOf("timer"),
            weight = 1.1
        ),
        "timer.resume" to IntentDef(
            keywords = setOf("resume", "continue", "timer"),
            phrases = listOf("resume timer", "continue timer", "resume the timer"),
            required = setOf("timer"),
            weight = 1.1
        ),
        "timer.check" to IntentDef(
            keywords = setOf("check", "timer", "timers", "how much", "time left"),
            phrases = listOf("check timer", "check timers", "how much time left", "time left", "timer status"),
            required = setOf("timer"),
            weight = 1.1
        ),

        // ── Connectivity ──
        "connectivity.wifi_on" to IntentDef(
            keywords = setOf("turn", "on", "enable", "switch", "activate", "wifi", "wireless"),
            phrases = listOf("turn on wifi", "enable wifi", "switch on wifi", "activate wifi", "wifi on", "turn wifi on"),
            required = setOf("wifi"),
            weight = 1.2
        ),
        "connectivity.wifi_off" to IntentDef(
            keywords = setOf("turn", "off", "disable", "switch", "deactivate", "wifi", "wireless"),
            phrases = listOf("turn off wifi", "disable wifi", "switch off wifi", "deactivate wifi", "wifi off", "turn wifi off"),
            required = setOf("wifi"),
            weight = 1.2
        ),
        "connectivity.bluetooth_on" to IntentDef(
            keywords = setOf("turn", "on", "enable", "switch", "activate", "bluetooth"),
            phrases = listOf("turn on bluetooth", "enable bluetooth", "switch on bluetooth", "activate bluetooth", "bluetooth on"),
            required = setOf("bluetooth"),
            weight = 1.2
        ),
        "connectivity.bluetooth_off" to IntentDef(
            keywords = setOf("turn", "off", "disable", "switch", "deactivate", "bluetooth"),
            phrases = listOf("turn off bluetooth", "disable bluetooth", "switch off bluetooth", "deactivate bluetooth", "bluetooth off"),
            required = setOf("bluetooth"),
            weight = 1.2
        ),
        "connectivity.hotspot_on" to IntentDef(
            keywords = setOf("turn", "on", "enable", "switch", "activate", "hotspot", "tethering"),
            phrases = listOf("turn on hotspot", "enable hotspot", "switch on hotspot", "mobile hotspot on", "tethering on"),
            required = setOf("hotspot", "tethering"),
            weight = 1.2
        ),
        "connectivity.hotspot_off" to IntentDef(
            keywords = setOf("turn", "off", "disable", "switch", "deactivate", "hotspot", "tethering"),
            phrases = listOf("turn off hotspot", "disable hotspot", "switch off hotspot", "mobile hotspot off", "tethering off"),
            required = setOf("hotspot", "tethering"),
            weight = 1.2
        ),
        "connectivity.mobile_data_on" to IntentDef(
            keywords = setOf("turn", "on", "enable", "mobile", "data", "cellular"),
            phrases = listOf("turn on mobile data", "enable mobile data", "cellular data on", "data on"),
            required = setOf("mobile", "data"),
            weight = 1.2
        ),
        "connectivity.mobile_data_off" to IntentDef(
            keywords = setOf("turn", "off", "disable", "mobile", "data", "cellular"),
            phrases = listOf("turn off mobile data", "disable mobile data", "cellular data off", "data off"),
            required = setOf("mobile", "data"),
            weight = 1.2
        ),
        "connectivity.status" to IntentDef(
            keywords = setOf("status", "wifi", "bluetooth", "hotspot", "connected", "connection"),
            phrases = listOf("wifi status", "bluetooth status", "connection status", "is wifi on", "is bluetooth on"),
            weight = 1.1
        ),
        "connectivity.network_info" to IntentDef(
            keywords = setOf("network", "internet", "connection", "ip", "address", "details", "info", "wifi", "status"),
            phrases = listOf(
                "network info", "network information", "network status", "network details",
                "internet info", "internet status", "connection info", "connection details",
                "ip address", "my ip", "wifi info", "wifi details", "mobile data info"
            ),
            // "network info" and friends were previously unrecognised; require a real
            // network token so ordinary commands never route here.
            required = setOf("network", "internet", "connection", "ip"),
            weight = 1.2
        ),

        // ── Settings ──
        "settings.open" to IntentDef(
            keywords = setOf("open", "settings", "setting", "go", "system"),
            phrases = listOf("open settings", "go to settings", "system settings", "settings", "show settings"),
            required = setOf("settings"),
            weight = 1.1
        ),
        "settings.wifi" to IntentDef(
            keywords = setOf("open", "wifi", "wireless", "settings"),
            phrases = listOf("wifi settings", "open wifi settings", "wireless settings"),
            required = setOf("wifi"),
            weight = 1.1
        ),
        "settings.bluetooth" to IntentDef(
            keywords = setOf("open", "bluetooth", "settings"),
            phrases = listOf("bluetooth settings", "open bluetooth settings"),
            required = setOf("bluetooth"),
            weight = 1.1
        ),
        "settings.display" to IntentDef(
            keywords = setOf("open", "display", "screen", "brightness", "settings"),
            phrases = listOf("display settings", "screen settings", "open display settings"),
            required = setOf("display", "screen"),
            weight = 1.1
        ),
        "settings.sound" to IntentDef(
            keywords = setOf("open", "sound", "volume", "audio", "ringtone", "settings"),
            phrases = listOf("sound settings", "volume settings", "audio settings", "open sound settings"),
            required = setOf("sound", "volume", "audio"),
            weight = 1.1
        ),
        "settings.battery" to IntentDef(
            keywords = setOf("open", "battery", "power", "saver", "settings"),
            phrases = listOf("battery settings", "power saving", "battery saver", "open battery settings"),
            required = setOf("battery"),
            weight = 1.1
        ),
        "settings.apps" to IntentDef(
            keywords = setOf("open", "manage", "apps", "applications", "installed", "settings"),
            phrases = listOf("app settings", "manage apps", "application settings", "installed apps", "open app settings"),
            required = setOf("apps", "applications"),
            weight = 1.1
        ),

        // ── Brightness / Volume / Flashlight / Battery / Storage ──
        "device.set_brightness" to IntentDef(
            keywords = setOf("set", "brightness", "level", "screen", "display", "to", "percent"),
            phrases = listOf("set brightness", "brightness to", "screen brightness", "adjust brightness", "set screen brightness"),
            required = setOf("brightness"),
            weight = 1.2
        ),
        "device.set_volume" to IntentDef(
            keywords = setOf("set", "volume", "level", "to", "up", "down", "increase", "decrease", "mute", "unmute", "media", "ring", "alarm"),
            phrases = listOf("set volume", "volume up", "volume down", "increase volume", "decrease volume", "mute", "unmute", "media volume", "ring volume"),
            required = setOf("volume", "mute", "unmute"),
            weight = 1.2
        ),
        "device.flashlight" to IntentDef(
            keywords = setOf("flashlight", "torch", "light", "on", "off", "toggle"),
            phrases = listOf("turn on flashlight", "turn off flashlight", "flashlight on", "flashlight off", "toggle flashlight", "torch on", "torch off"),
            required = setOf("flashlight", "torch"),
            weight = 1.2
        ),
        "device.battery_status" to IntentDef(
            keywords = setOf("battery", "status", "level", "percent", "power", "charge"),
            phrases = listOf("battery status", "battery level", "battery percent", "power status", "check battery"),
            required = setOf("battery"),
            weight = 1.1
        ),
        "device.storage_status" to IntentDef(
            keywords = setOf("storage", "status", "space", "memory", "free", "used"),
            phrases = listOf("storage status", "storage space", "memory status", "free space", "check storage"),
            required = setOf("storage", "memory"),
            weight = 1.1
        ),

        // ── Device Control ──
        "device.airplane_mode" to IntentDef(
            keywords = setOf("airplane", "mode", "flight", "on", "off", "enable", "disable"),
            phrases = listOf("airplane mode on", "airplane mode off", "enable airplane mode", "disable airplane mode", "flight mode"),
            required = setOf("airplane", "flight"),
            weight = 1.2
        ),
        "device.do_not_disturb" to IntentDef(
            keywords = setOf("do", "not", "disturb", "dnd", "silent", "mode", "on", "off"),
            phrases = listOf("do not disturb", "dnd on", "dnd off", "silent mode", "enable do not disturb", "disable do not disturb"),
            required = setOf("disturb", "dnd", "silent"),
            weight = 1.2
        ),
        "device.lock_screen" to IntentDef(
            keywords = setOf("lock", "screen", "lockscreen"),
            phrases = listOf("lock screen", "lock the screen", "lockscreen"),
            required = setOf("lock"),
            weight = 1.1
        ),
        "device.take_screenshot" to IntentDef(
            keywords = setOf("screenshot", "capture", "screen", "take"),
            phrases = listOf("take screenshot", "capture screen", "screenshot", "take a screenshot"),
            required = setOf("screenshot", "capture"),
            weight = 1.1
        ),
        "device.open_camera" to IntentDef(
            keywords = setOf("open", "camera", "launch", "start"),
            phrases = listOf("open camera", "launch camera", "start camera", "camera"),
            required = setOf("camera"),
            weight = 1.1
        ),

        // ── App Control ──
        "app.open" to IntentDef(
            keywords = setOf("open", "launch", "start", "run", "show", "go", "take", "app", "application"),
            phrases = listOf("open", "launch", "start", "run", "show", "go to", "take me to", "open app", "launch app"),
            weight = 1.0
        ),
        "app.close" to IntentDef(
            keywords = setOf("close", "quit", "exit", "kill", "stop", "app"),
            phrases = listOf("close", "quit", "exit", "kill", "stop app", "close app"),
            required = setOf("close", "quit", "exit"),
            weight = 1.1
        ),
        "app.switch" to IntentDef(
            keywords = setOf("switch", "to", "app", "application"),
            phrases = listOf("switch to", "switch app", "change app"),
            required = setOf("switch"),
            weight = 1.0
        ),

        // ── Communication ──
        "comm.call" to IntentDef(
            keywords = setOf("call", "dial", "phone", "ring", "contact"),
            phrases = listOf("call", "dial", "phone", "ring", "call contact", "phone contact"),
            required = setOf("call", "dial", "phone", "ring"),
            weight = 1.2
        ),
        "comm.send_sms" to IntentDef(
            keywords = setOf("send", "sms", "text", "message", "to"),
            phrases = listOf("send sms", "send text", "send message", "text message", "sms message"),
            required = setOf("send", "sms", "text", "message"),
            weight = 1.2
        ),
        "comm.send_whatsapp" to IntentDef(
            keywords = setOf("send", "whatsapp", "message", "to", "chat"),
            phrases = listOf("send whatsapp", "send whatsapp message", "whatsapp message", "chat on whatsapp", "whatsapp chat"),
            required = setOf("whatsapp"),
            weight = 1.2
        ),
        "comm.send_email" to IntentDef(
            keywords = setOf("send", "email", "mail", "message", "to"),
            phrases = listOf("send email", "send mail", "email message", "send an email"),
            required = setOf("email", "mail"),
            weight = 1.1
        ),
        "comm.read_messages" to IntentDef(
            keywords = setOf("read", "show", "messages", "sms", "text", "unread"),
            phrases = listOf("read messages", "show messages", "read sms", "read texts", "unread messages"),
            required = setOf("read", "messages", "sms", "text"),
            weight = 1.1
        ),

        // ── Web Search ──
        "web.search" to IntentDef(
            keywords = setOf("search", "google", "look", "up", "find", "web", "browser"),
            phrases = listOf("search", "search for", "google", "look up", "search google for", "search the web", "find", "browse"),
            weight = 0.9
        ),

        // ── Information ──
        "info.weather" to IntentDef(
            keywords = setOf("weather", "temperature", "forecast", "climate", "rain", "sunny", "cloudy", "hot", "cold"),
            phrases = listOf("weather", "weather today", "weather tomorrow", "temperature", "forecast", "what is the weather", "weather in"),
            required = setOf("weather", "temperature", "forecast"),
            weight = 1.2
        ),
        "info.news" to IntentDef(
            keywords = setOf("news", "headlines", "latest", "today", "current"),
            phrases = listOf("news", "latest news", "headlines", "news today", "current news"),
            required = setOf("news", "headlines"),
            weight = 1.1
        ),
        "info.sports" to IntentDef(
            keywords = setOf("sports", "score", "game", "match", "team", "league"),
            phrases = listOf("sports score", "game score", "match score", "team score", "sports news"),
            required = setOf("sports", "score", "game", "match"),
            weight = 1.1
        ),
        "info.stock" to IntentDef(
            keywords = setOf("stock", "share", "price", "market", "trade"),
            phrases = listOf("stock price", "share price", "market price", "stock quote", "trade price"),
            required = setOf("stock", "share", "market"),
            weight = 1.1
        ),
        "info.definition" to IntentDef(
            keywords = setOf("define", "definition", "meaning", "what is", "means"),
            phrases = listOf("define", "definition of", "meaning of", "what is the meaning", "what does", "means"),
            required = setOf("define", "definition", "meaning"),
            weight = 1.3
        ),
        "info.translate" to IntentDef(
            keywords = setOf("translate", "translation", "language", "to", "from"),
            phrases = listOf("translate", "translate to", "translation", "language translate"),
            required = setOf("translate", "translation"),
            weight = 1.1
        ),

        // ── Media ──
        "media.play" to IntentDef(
            keywords = setOf("play", "music", "song", "artist", "playlist", "resume", "start"),
            phrases = listOf("play music", "play song", "play artist", "play playlist", "resume music", "start music"),
            required = setOf("play", "music", "song", "resume"),
            weight = 1.2
        ),
        "media.pause" to IntentDef(
            keywords = setOf("pause", "music", "stop", "media"),
            phrases = listOf("pause music", "pause", "stop music", "pause media"),
            required = setOf("pause", "stop"),
            weight = 1.2
        ),
        "media.next" to IntentDef(
            keywords = setOf("next", "skip", "track", "song"),
            phrases = listOf("next track", "next song", "skip track", "skip song"),
            required = setOf("next", "skip"),
            weight = 1.1
        ),
        "media.previous" to IntentDef(
            keywords = setOf("previous", "prev", "back", "track", "song"),
            phrases = listOf("previous track", "previous song", "prev track", "back track"),
            required = setOf("previous", "prev", "back"),
            weight = 1.1
        ),
        "media.stop" to IntentDef(
            keywords = setOf("stop", "music", "media"),
            phrases = listOf("stop music", "stop media", "stop playing"),
            required = setOf("stop"),
            weight = 1.1
        ),
        "media.set_volume" to IntentDef(
            keywords = setOf("set", "music", "volume", "level", "to", "up", "down"),
            phrases = listOf("set music volume", "music volume up", "music volume down", "media volume"),
            required = setOf("music", "volume", "media"),
            weight = 1.1
        ),

        // ── Productivity ──
        "productivity.create_reminder" to IntentDef(
            keywords = setOf("remind", "reminder", "remember", "at", "on", "tomorrow", "today"),
            phrases = listOf("remind me", "create reminder", "set reminder", "reminder", "remember to", "remind me at", "remind me on"),
            required = setOf("remind", "reminder", "remember"),
            weight = 1.2
        ),
        "productivity.list_reminders" to IntentDef(
            keywords = setOf("list", "show", "my", "reminders", "pending"),
            phrases = listOf("list reminders", "show reminders", "my reminders", "pending reminders", "check reminders"),
            required = setOf("reminders", "reminder"),
            weight = 1.1
        ),
        "productivity.delete_reminder" to IntentDef(
            keywords = setOf("delete", "remove", "cancel", "reminder", "reminders"),
            phrases = listOf("delete reminder", "remove reminder", "cancel reminder", "delete all reminders"),
            required = setOf("delete", "remove", "cancel"),
            // "delete note" must reach productivity.delete_note, not reminders — the two
            // are separate stores, and reporting "no reminders" for a note was the bug.
            excludes = setOf("note", "notes"),
            weight = 1.1
        ),
        "productivity.delete_note" to IntentDef(
            keywords = setOf("delete", "remove", "clear", "cancel", "note", "notes"),
            phrases = listOf(
                "delete note", "delete notes", "remove note", "remove my note",
                "clear notes", "clear my notes", "delete all notes", "delete my note",
                "remove my notes"
            ),
            // Require a delete verb (OR), not the word "note": with "note" required,
            // "add note buy milk" scored just as high as a real delete and won.
            required = setOf("delete", "remove", "clear", "cancel"),
            weight = 1.1
        ),
        "productivity.create_note" to IntentDef(
            keywords = setOf("note", "notes", "write", "create", "add", "save", "memo"),
            phrases = listOf(
                "create note", "write note", "save note", "new note", "make a note",
                "take a note", "add note", "add a note", "add to my notes"
            ),
            required = setOf("note", "notes"),
            weight = 1.1
        ),
        "productivity.list_notes" to IntentDef(
            keywords = setOf("list", "show", "my", "notes"),
            phrases = listOf("list notes", "show notes", "my notes", "all notes"),
            required = setOf("notes"),
            weight = 1.1
        ),
        "productivity.search_notes" to IntentDef(
            keywords = setOf("search", "find", "note", "notes"),
            phrases = listOf("search notes", "find note", "search my notes", "find my notes"),
            required = setOf("search", "find"),
            weight = 1.1
        ),
        "productivity.create_calendar_event" to IntentDef(
            keywords = setOf("calendar", "event", "meeting", "appointment", "schedule", "add"),
            phrases = listOf("create calendar event", "add event", "schedule meeting", "add appointment", "calendar event"),
            required = setOf("calendar", "event", "meeting", "appointment", "schedule"),
            weight = 1.1
        ),
        "productivity.get_calendar_events" to IntentDef(
            keywords = setOf("calendar", "events", "schedule", "agenda", "today", "tomorrow"),
            phrases = listOf("calendar events", "my schedule", "agenda", "today schedule", "tomorrow schedule", "show calendar"),
            required = setOf("calendar", "events", "schedule", "agenda"),
            weight = 1.1
        ),

        // ── Calculation ──
        "calc.calculate" to IntentDef(
            keywords = setOf("calculate", "compute", "solve", "math", "plus", "minus", "times", "divided", "multiplied"),
            phrases = listOf("calculate", "compute", "solve", "what is", "how much is", "math", "plus", "minus", "times", "divided by", "multiplied by"),
            // A bare "what is" is far too generic (it appears in "what is the president",
            // "what is the weather"), so require a real arithmetic token to match.
            required = setOf("calculate", "compute", "solve", "math", "plus", "minus", "times", "divided", "multiplied"),
            excludes = setOf("meaning", "define", "definition", "mean", "weather", "news", "translate", "time", "date"),
            weight = 1.2
        ),
        "calc.convert_units" to IntentDef(
            keywords = setOf("convert", "unit", "to", "from", "in", "kilometer", "mile", "celsius", "fahrenheit", "kg", "pound"),
            phrases = listOf("convert", "convert to", "unit conversion", "in", "kilometer to mile", "celsius to fahrenheit"),
            required = setOf("convert"),
            weight = 1.1
        ),

        // ── Navigation ──
        "nav.open_maps" to IntentDef(
            keywords = setOf("maps", "map", "navigation", "navigate", "google maps", "directions"),
            phrases = listOf("open maps", "open map", "google maps", "navigation", "navigate", "show map"),
            required = setOf("maps", "map", "navigation", "navigate"),
            weight = 1.1
        ),
        "nav.get_directions" to IntentDef(
            keywords = setOf("directions", "to", "from", "how", "get", "route", "navigate", "locate", "take", "way"),
            phrases = listOf(
                "directions to", "how to get to", "route to", "navigate to", "get directions",
                "locate me to", "take me to", "how do i get to", "show me the way to", "go to"
            ),
            required = setOf("directions", "route", "navigate", "locate", "take"),
            weight = 1.1
        ),

        // ── System ──
        "system.shutdown" to IntentDef(
            keywords = setOf("shutdown", "shut down", "power off", "turn off"),
            phrases = listOf("shutdown", "shut down", "power off", "turn off phone", "turn off device"),
            required = setOf("shutdown", "shut", "power", "off"),
            weight = 1.2
        ),
        "system.restart" to IntentDef(
            keywords = setOf("restart", "reboot", "restart phone", "restart device"),
            phrases = listOf("restart", "reboot", "restart phone", "restart device"),
            required = setOf("restart", "reboot"),
            weight = 1.2
        ),
        "unknown" to IntentDef(weight = 0.1)
    )

    private val intentPatterns: Map<String, IntentDef> = intents.mapKeys { it.key }

    /**
     * Classify a query. Returns (intentName, confidence, allScores).
     * intentName is one of the keys above, or "unknown".
     */
    fun classify(contentTokens: List<String>, rawQuery: String): ClassificationResult {
        val tokenSet = contentTokens.toSet()
        val tokenList = contentTokens
        val joined = contentTokens.joinToString(" ")
        val rawLow = rawQuery.lowercase(Locale.ROOT)
        val scores = mutableMapOf<String, Double>()

        for ((intentName, def) in intentPatterns) {
            var score = 0.0

            // 1. Required tokens: at least one must be present (they are synonyms),
            //    otherwise heavily penalize (except unknown).
            if (intentName != "unknown" && def.required.isNotEmpty()) {
                val nonePresent = def.required.none { it in tokenSet }
                if (nonePresent) {
                    scores[intentName] = 0.0
                    continue
                }
            }

            // 2. Keyword coverage
            val keywordHits = def.keywords.intersect(tokenSet).size
            val keywordCoverage = if (def.keywords.isEmpty()) 0.0 else keywordHits.toDouble() / def.keywords.size
            score += keywordCoverage * 0.35

            // 3. Exact phrase match (substring in raw query)
            val phraseHit = def.phrases.any { rawLow.contains(it.lowercase(Locale.ROOT)) }
            if (phraseHit) score += 0.30

            // 4. Regex match
            val regexHit = def.regexes.any {
                it.matcher(rawLow).matches() || it.matcher(rawLow).find()
            }
            if (regexHit) score += 0.20

            // 5. Exclusion penalty
            val exclusionHits = def.excludes.intersect(tokenSet).size
            score -= exclusionHits * 0.15

            // 6. Weight
            score *= def.weight

            // 7. Length penalty
            val lenPen = when {
                tokenList.size <= 2 -> 0.7
                tokenList.size <= 5 -> 0.9
                else -> 1.0
            }
            score *= lenPen

            scores[intentName] = score.coerceIn(0.0, 1.0)
        }

        // Fuzzy fallback for low-scoring intents
        val bestScored = scores.entries.maxByOrNull { it.value }
        if (bestScored == null || bestScored.value < CONFIDENCE_THRESHOLD) {
            val fuzzyResults = mutableMapOf<String, Double>()
            for ((intentName, def) in intentPatterns) {
                if (intentName == "unknown") continue
                // Honor required tokens in the fuzzy path too, otherwise unrelated
                // phrases (e.g. "turn on torch" ~ "turn on bluetooth") can win.
                if (def.required.isNotEmpty() && def.required.none { it in tokenSet }) continue
                val (bestPhrase, fuzzyScore) = FuzzyMatcher.bestMatch(joined, def.phrases)
                if (fuzzyScore >= FUZZY_THRESHOLD) {
                    fuzzyResults[intentName] = fuzzyScore * def.weight
                }
            }
            val bestFuzzy = fuzzyResults.entries.maxByOrNull { it.value }
            if (bestFuzzy != null && bestFuzzy.value >= CONFIDENCE_THRESHOLD) {
                // Merge fuzzy with existing scores
                for ((name, score) in fuzzyResults) {
                    scores[name] = maxOf(scores[name] ?: 0.0, score)
                }
            }
        }

        // Renormalize
        val maxScore = scores.values.maxOrNull() ?: 0.0
        val normalized = scores.mapValues { it.value / maxOf(maxScore, 0.001) }
            .mapValues { it.value.coerceIn(0.0, 1.0) }

        val best = normalized.entries.maxByOrNull { it.value }
        val intentName = if (best != null && best.value >= CONFIDENCE_THRESHOLD) best.key else "unknown"
        val confidence = best?.value ?: 0.0

        return Triple(intentName, confidence, normalized)
    }

    fun getKnownPhrases(): List<String> = intents.values.flatMap { it.phrases }.distinct()
    fun getIntentKeywords(intent: String): Set<String> = intents[intent]?.keywords ?: emptySet()
}