package com.ev.ai.assistant.nlu

import org.junit.Assert.*
import org.junit.Test

class EvNluEngineTest {

    // ════════════════════════════════════════════════════════════════════════════
    // Wake-word stripping
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    fun wakeWordStripping() {
        assertEquals("what time is it", EvNluEngine.stripWakePrefixes("hey ev what time is it"))
        assertEquals("what time is it", EvNluEngine.stripWakePrefixes("ok jarvis what time is it"))
        assertEquals("set alarm 7 am", EvNluEngine.stripWakePrefixes("please set alarm 7 am"))
        assertEquals("open whatsapp", EvNluEngine.stripWakePrefixes("ev, open whatsapp"))
        assertEquals("hello", EvNluEngine.stripWakePrefixes("hello"))
        assertEquals("turn on bluetooth", EvNluEngine.stripWakePrefixes("can you turn on bluetooth"))
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Conversation / Greetings (the exact-match bug fix)
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    fun greetingsAreRecognized() {
        val greetings = listOf(
            "hi", "hello", "hey", "hi there", "hello ev", "hey there",
            "good morning", "good afternoon", "good evening", "good night",
            "hey ev", "hello jarvis", "ok friday"
        )
        for (g in greetings) {
            val intent = EvNluEngine.parse(g)
            assertTrue("'$g' should be a greeting, got $intent", intent is EvIntent.Conversation)
        }
    }

    @Test
    fun whoAreYouVariations() {
        val queries = listOf(
            "who are you", "what are you", "your name", "are you ev",
            "are you jarvis", "who r u", "whats your name", "identify yourself"
        )
        for (q in queries) {
            val intent = EvNluEngine.parse(q)
            assertTrue("'$q' should be who_are_you, got $intent", intent is EvIntent.Conversation)
        }
    }

    @Test
    fun capabilitiesQuery() {
        val intent = EvNluEngine.parse("what can you do")
        assertTrue(intent is EvIntent.Conversation)
        val help = EvNluEngine.parse("help me")
        assertTrue(help is EvIntent.Help || help is EvIntent.Conversation)
    }

    @Test
    fun thanksVariations() {
        for (q in listOf("thanks", "thank you", "thank you so much", "thanks ev", "cheers")) {
            val intent = EvNluEngine.parse(q)
            assertTrue("'$q' should be thanks, got $intent", intent is EvIntent.Conversation)
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Time & Date
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    fun timeQueries() {
        val queries = listOf(
            "what time is it", "current time", "tell me the time", "time now",
            "whats the time", "what is the time", "the time", "check time",
            "time", "what's the time", "do you know the time"
        )
        for (q in queries) {
            val intent = EvNluEngine.parse(q)
            assertTrue("'$q' should be GetTime, got $intent", intent is EvIntent.GetTime)
        }
    }

    @Test
    fun timeNotConfusedWithTimer() {
        // "set timer" should NOT match time
        val intent = EvNluEngine.parse("set timer 5 minutes")
        assertTrue("'set timer 5 minutes' should be a timer, got $intent", intent is EvIntent.SetTimer)

        val intent2 = EvNluEngine.parse("set alarm 7 am")
        assertTrue("'set alarm 7 am' should be an alarm, got $intent2", intent2 is EvIntent.SetAlarm)
    }

    @Test
    fun dateQueries() {
        val queries = listOf(
            "todays date", "what date", "current date", "what day", "day today",
            "what is today", "today is", "date today", "whats today", "date"
        )
        for (q in queries) {
            val intent = EvNluEngine.parse(q)
            assertTrue("'$q' should be GetDate, got $intent", intent is EvIntent.GetDate)
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Alarms
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    fun alarmSetting() {
        val cases = mapOf(
            "set alarm 7" to Pair(7, 0),
            "set alarm 7 am" to Pair(7, 0),
            "set alarm at 5 pm" to Pair(17, 0),
            "set alarm for 7:30 am" to Pair(7, 30),
            "wake me up at 6" to Pair(6, 0),
            "wake me at 6:30" to Pair(6, 30),
            "set alarm for 8 pm" to Pair(20, 0)
        )
        for ((query, expected) in cases) {
            val intent = EvNluEngine.parse(query)
            assertTrue("'$query' should be SetAlarm, got $intent", intent is EvIntent.SetAlarm)
            val alarm = intent as EvIntent.SetAlarm
            assertEquals("'$query' hour", expected.first, alarm.hour)
            assertEquals("'$query' minute", expected.second, alarm.minute)
        }
    }

    @Test
    fun alarmDeleteEditSnooze() {
        assertTrue(EvNluEngine.parse("delete alarm") is EvIntent.DeleteAlarm)
        assertTrue(EvNluEngine.parse("remove alarm") is EvIntent.DeleteAlarm)
        assertTrue(EvNluEngine.parse("cancel alarm") is EvIntent.DeleteAlarm)
        assertTrue(EvNluEngine.parse("snooze") is EvIntent.SnoozeAlarm)
        assertTrue(EvNluEngine.parse("snooze alarm") is EvIntent.SnoozeAlarm)
        assertTrue(EvNluEngine.parse("edit alarm") is EvIntent.EditAlarm)
        assertTrue(EvNluEngine.parse("check alarm") is EvIntent.CheckAlarm)
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Timers
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    fun timerSetting() {
        val cases = mapOf(
            "set timer 5 minutes" to 300,
            "set a timer for 30 sec" to 30,
            "10 minute timer" to 600,
            "timer 1 hour" to 3600,
            "set timer 10 mins" to 600,
            "countdown 2 minutes" to 120
        )
        for ((query, expectedSeconds) in cases) {
            val intent = EvNluEngine.parse(query)
            assertTrue("'$query' should be SetTimer, got $intent", intent is EvIntent.SetTimer)
            assertEquals("'$query' seconds", expectedSeconds, (intent as EvIntent.SetTimer).durationSeconds)
        }
    }

    @Test
    fun timerControl() {
        assertTrue(EvNluEngine.parse("cancel timer") is EvIntent.CancelTimer)
        assertTrue(EvNluEngine.parse("pause timer") is EvIntent.PauseTimer)
        assertTrue(EvNluEngine.parse("resume timer") is EvIntent.ResumeTimer)
        assertTrue(EvNluEngine.parse("check timer") is EvIntent.CheckTimer)
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Connectivity
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    fun wifiControl() {
        assertEquals(
            EvIntent.ToggleConnectivity(ConnectivityType.WIFI, true),
            EvNluEngine.parse("turn on wifi")
        )
        assertEquals(
            EvIntent.ToggleConnectivity(ConnectivityType.WIFI, false),
            EvNluEngine.parse("turn off wifi")
        )
        assertTrue(EvNluEngine.parse("enable wifi") is EvIntent.ToggleConnectivity)
        assertTrue(EvNluEngine.parse("disable wifi") is EvIntent.ToggleConnectivity)
        assertTrue(EvNluEngine.parse("wifi status") is EvIntent.GetConnectivityStatus)
    }

    @Test
    fun bluetoothControl() {
        assertEquals(
            EvIntent.ToggleConnectivity(ConnectivityType.BLUETOOTH, true),
            EvNluEngine.parse("turn on bluetooth")
        )
        assertEquals(
            EvIntent.ToggleConnectivity(ConnectivityType.BLUETOOTH, false),
            EvNluEngine.parse("turn off bluetooth")
        )
    }

    @Test
    fun hotspotControl() {
        assertEquals(
            EvIntent.ToggleConnectivity(ConnectivityType.HOTSPOT, true),
            EvNluEngine.parse("turn on hotspot")
        )
        assertEquals(
            EvIntent.ToggleConnectivity(ConnectivityType.HOTSPOT, false),
            EvNluEngine.parse("turn off hotspot")
        )
    }

    @Test
    fun mobileDataControl() {
        assertTrue(EvNluEngine.parse("turn on mobile data") is EvIntent.ToggleConnectivity)
        assertTrue(EvNluEngine.parse("turn off mobile data") is EvIntent.ToggleConnectivity)
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Settings
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    fun settingsQueries() {
        assertTrue(EvNluEngine.parse("open settings") is EvIntent.OpenSettings)
        assertTrue(EvNluEngine.parse("wifi settings") is EvIntent.OpenSettings)
        assertTrue(EvNluEngine.parse("bluetooth settings") is EvIntent.OpenSettings)
        assertTrue(EvNluEngine.parse("display settings") is EvIntent.OpenSettings)
        assertTrue(EvNluEngine.parse("sound settings") is EvIntent.OpenSettings)
        assertTrue(EvNluEngine.parse("battery settings") is EvIntent.OpenSettings)
        assertTrue(EvNluEngine.parse("app settings") is EvIntent.OpenSettings)
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Device Control
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    fun brightnessControl() {
        assertTrue(EvNluEngine.parse("set brightness 50") is EvIntent.SetBrightness)
        assertTrue(EvNluEngine.parse("brightness to 80 percent") is EvIntent.SetBrightness)
        assertTrue(EvNluEngine.parse("adjust brightness") is EvIntent.SetBrightness)
    }

    @Test
    fun volumeControl() {
        assertTrue(EvNluEngine.parse("set volume 70") is EvIntent.SetVolume)
        assertTrue(EvNluEngine.parse("volume up") is EvIntent.SetVolume)
        assertTrue(EvNluEngine.parse("volume down") is EvIntent.SetVolume)
        assertTrue(EvNluEngine.parse("increase media volume") is EvIntent.SetVolume)
        assertTrue(EvNluEngine.parse("mute") is EvIntent.SetVolume)
    }

    @Test
    fun flashlightControl() {
        assertTrue(EvNluEngine.parse("turn on flashlight") is EvIntent.ToggleFlashlight)
        assertTrue(EvNluEngine.parse("turn off flashlight") is EvIntent.ToggleFlashlight)
        assertTrue(EvNluEngine.parse("flashlight on") is EvIntent.ToggleFlashlight)
        assertTrue(EvNluEngine.parse("torch off") is EvIntent.ToggleFlashlight)
    }

    @Test
    fun deviceStatus() {
        assertTrue(EvNluEngine.parse("battery status") is EvIntent.GetBatteryStatus)
        assertTrue(EvNluEngine.parse("battery level") is EvIntent.GetBatteryStatus)
        assertTrue(EvNluEngine.parse("storage status") is EvIntent.GetStorageStatus)
        assertTrue(EvNluEngine.parse("airplane mode on") is EvIntent.SetAirplaneMode)
        assertTrue(EvNluEngine.parse("do not disturb") is EvIntent.SetDoNotDisturb)
        assertTrue(EvNluEngine.parse("lock screen") is EvIntent.LockScreen)
        assertTrue(EvNluEngine.parse("take screenshot") is EvIntent.TakeScreenshot)
        assertTrue(EvNluEngine.parse("open camera") is EvIntent.OpenCamera)
    }

    // ════════════════════════════════════════════════════════════════════════════
    // App Control
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    fun appControl() {
        assertTrue(EvNluEngine.parse("open whatsapp") is EvIntent.OpenApp)
        assertTrue(EvNluEngine.parse("launch youtube") is EvIntent.OpenApp)
        assertTrue(EvNluEngine.parse("start chrome") is EvIntent.OpenApp)
        assertTrue(EvNluEngine.parse("run spotify") is EvIntent.OpenApp)
        assertTrue(EvNluEngine.parse("open camera app") is EvIntent.OpenApp)
        assertTrue(EvNluEngine.parse("close whatsapp") is EvIntent.CloseApp)
        assertTrue(EvNluEngine.parse("switch to spotify") is EvIntent.SwitchApp)
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Communication
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    fun callControl() {
        assertTrue(EvNluEngine.parse("call mom") is EvIntent.MakeCall)
        assertTrue(EvNluEngine.parse("dial 5551234") is EvIntent.MakeCall)
        assertTrue(EvNluEngine.parse("phone john") is EvIntent.MakeCall)
        assertTrue(EvNluEngine.parse("ring sarah") is EvIntent.MakeCall)
    }

    @Test
    fun smsAndWhatsApp() {
        assertTrue(EvNluEngine.parse("send sms to mom hello") is EvIntent.SendSms)
        assertTrue(EvNluEngine.parse("send text to john hi") is EvIntent.SendSms)
        assertTrue(EvNluEngine.parse("send whatsapp message saying hello") is EvIntent.SendWhatsAppMessage)
        assertTrue(EvNluEngine.parse("open whatsapp") is EvIntent.OpenApp)
        assertTrue(EvNluEngine.parse("read messages") is EvIntent.ReadMessages)
        assertTrue(EvNluEngine.parse("read unread messages") is EvIntent.ReadMessages)
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Web Search
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    fun webSearch() {
        val intent = EvNluEngine.parse("search for weather london")
        assertTrue(intent is EvIntent.SearchWeb)
        val search = intent as EvIntent.SearchWeb
        assertTrue(search.query.contains("weather"))

        val intent2 = EvNluEngine.parse("google best restaurants")
        assertTrue(intent2 is EvIntent.SearchWeb)

        val intent3 = EvNluEngine.parse("look up python tutorial")
        assertTrue(intent3 is EvIntent.SearchWeb)
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Information
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    fun informationQueries() {
        assertTrue(EvNluEngine.parse("weather today") is EvIntent.GetWeather)
        assertTrue(EvNluEngine.parse("weather in london") is EvIntent.GetWeather)
        assertTrue(EvNluEngine.parse("weather tomorrow") is EvIntent.GetWeather)
        assertTrue(EvNluEngine.parse("latest news") is EvIntent.GetNews)
        assertTrue(EvNluEngine.parse("sports score") is EvIntent.GetSportsScore)
        assertTrue(EvNluEngine.parse("stock price apple") is EvIntent.GetStockPrice)
        assertTrue(EvNluEngine.parse("define hello") is EvIntent.GetDefinition)
        assertTrue(EvNluEngine.parse("translate hello to spanish") is EvIntent.Translate)
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Media
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    fun mediaControl() {
        assertTrue(EvNluEngine.parse("play music") is EvIntent.PlayMusic)
        assertTrue(EvNluEngine.parse("play spotify") is EvIntent.PlayMusic)
        assertTrue(EvNluEngine.parse("play song shape of you") is EvIntent.PlayMusic)
        assertTrue(EvNluEngine.parse("pause music") is EvIntent.PauseMusic)
        assertTrue(EvNluEngine.parse("pause") is EvIntent.PauseMusic)
        assertTrue(EvNluEngine.parse("resume music") is EvIntent.ResumeMusic)
        assertTrue(EvNluEngine.parse("next track") is EvIntent.NextTrack)
        assertTrue(EvNluEngine.parse("next song") is EvIntent.NextTrack)
        assertTrue(EvNluEngine.parse("previous track") is EvIntent.PreviousTrack)
        assertTrue(EvNluEngine.parse("stop music") is EvIntent.StopMusic)
        assertTrue(EvNluEngine.parse("set music volume 50") is EvIntent.SetMusicVolume)
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Productivity
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    fun productivityQueries() {
        assertTrue(EvNluEngine.parse("remind me to call mom at 5 pm") is EvIntent.CreateReminder)
        assertTrue(EvNluEngine.parse("create reminder meeting tomorrow") is EvIntent.CreateReminder)
        assertTrue(EvNluEngine.parse("list reminders") is EvIntent.ListReminders)
        assertTrue(EvNluEngine.parse("delete reminder") is EvIntent.DeleteReminder)
        assertTrue(EvNluEngine.parse("create note buy milk") is EvIntent.CreateNote)
        assertTrue(EvNluEngine.parse("list notes") is EvIntent.ListNotes)
        assertTrue(EvNluEngine.parse("search notes meeting") is EvIntent.SearchNotes)
        assertTrue(EvNluEngine.parse("add calendar event meeting") is EvIntent.AddCalendarEvent)
        assertTrue(EvNluEngine.parse("calendar events today") is EvIntent.GetCalendarEvents)
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Calculation
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    fun calculationQueries() {
        val cases = mapOf(
            "15 times 4" to 60.0,
            "2 plus 3" to 5.0,
            "10 minus 3" to 7.0,
            "6 divided by 2" to 3.0,
            "calculate 8 times 7" to 56.0,
            "what is 5 plus 5" to 10.0,
            "how much is 9 times 9" to 81.0
        )
        for ((query, expected) in cases) {
            val intent = EvNluEngine.parse(query)
            assertTrue("'$query' should be Calculate, got $intent", intent is EvIntent.Calculate)
            assertEquals("'$query' result", expected, (intent as EvIntent.Calculate).result, 0.001)
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Navigation
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    fun navigationQueries() {
        assertTrue(EvNluEngine.parse("open maps") is EvIntent.OpenMaps)
        assertTrue(EvNluEngine.parse("google maps") is EvIntent.OpenMaps)
        assertTrue(EvNluEngine.parse("directions to new york") is EvIntent.GetDirections)
        assertTrue(EvNluEngine.parse("navigate to london") is EvIntent.GetDirections)
    }

    // ════════════════════════════════════════════════════════════════════════════
    // System
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    fun systemQueries() {
        assertTrue(EvNluEngine.parse("shutdown") is EvIntent.Shutdown)
        assertTrue(EvNluEngine.parse("restart") is EvIntent.Restart)
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Unknown / fallback
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    fun unknownQueries() {
        val intent = EvNluEngine.parse("do something unusual and random")
        assertTrue(intent is EvIntent.Unknown)
    }

    @Test
    fun emptyQuery() {
        val intent = EvNluEngine.parse("")
        assertTrue(intent is EvIntent.Conversation)

        val intent2 = EvNluEngine.parse("   ")
        assertTrue(intent2 is EvIntent.Conversation)
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Confidence / Accuracy regression
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    fun confidenceAboveThresholdForCommonQueries() {
        val commonQueries = listOf(
            "what time is it", "open whatsapp", "set alarm 7 am", "set timer 5 minutes",
            "turn on wifi", "turn off bluetooth", "call mom", "play music",
            "weather today", "calculate 5 plus 5", "battery status", "open settings"
        )
        for (q in commonQueries) {
            val intent = EvNluEngine.parse(q)
            val isKnown = intent is EvIntent.Unknown && (intent as EvIntent.Unknown).confidence < 0.45
            assertFalse("'$q' should not be low-confidence unknown, got $intent", isKnown)
        }
    }

    @Test
    fun wakeWordDoesNotBreakIntent() {
        val plain = EvNluEngine.parse("set alarm 7 am")
        val withWake = EvNluEngine.parse("hey ev set alarm 7 am")
        assertEquals(plain::class, withWake::class)

        val plain2 = EvNluEngine.parse("what time is it")
        val withWake2 = EvNluEngine.parse("ok jarvis what time is it")
        assertEquals(plain2::class, withWake2::class)
    }
}