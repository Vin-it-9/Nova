package com.ev.ai.assistant.nlu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the second batch of on-device bugs:
 *  - knowledge questions ("who is the president") hijacked by the identity reply
 *  - calendar queries returning the identity reply instead of real events
 *  - "delete note" reporting "no reminders to delete"
 *  - "network info" being unrecognised
 *  - "play <song>" only opening a search instead of playing
 *  - invalid commands dumping a "Try: ..." list
 */
class EvNluEngineBugBatch2Test {

    // ── Knowledge questions must not become the identity line ───────────────
    @Test
    fun presidentQuestionIsAnsweredFromWeb() {
        assertTrue(EvNluEngine.parse("who is the president") is EvIntent.Answer)
        assertTrue(EvNluEngine.parse("who is the president of usa") is EvIntent.Answer)
        assertTrue(EvNluEngine.parse("president of usa") is EvIntent.Answer)
        assertTrue(EvNluEngine.parse("capital of france") is EvIntent.Answer)
    }

    @Test
    fun knowledgeAnswerCarriesTheOriginalQuery() {
        val intent = EvNluEngine.parse("who is the president")
        assertTrue(intent is EvIntent.Answer)
        assertEquals("who is the president", (intent as EvIntent.Answer).query)
    }

    // ── Identity queries still work ─────────────────────────────────────────
    @Test
    fun identityQueriesStillReturnConversation() {
        assertTrue(EvNluEngine.parse("who are you") is EvIntent.Conversation)
        assertTrue(EvNluEngine.parse("what are you") is EvIntent.Conversation)
        assertTrue(EvNluEngine.parse("your name") is EvIntent.Conversation)
        assertTrue(EvNluEngine.parse("who r u") is EvIntent.Conversation)
    }

    // ── Calendar ────────────────────────────────────────────────────────────
    @Test
    fun calendarQueriesReturnCalendarEvents() {
        assertTrue(EvNluEngine.parse("what's on my calendar") is EvIntent.GetCalendarEvents)
        assertTrue(EvNluEngine.parse("what is on my calendar") is EvIntent.GetCalendarEvents)
        assertTrue(EvNluEngine.parse("whats on my calendar") is EvIntent.GetCalendarEvents)
        // Common misspelling must be handled too.
        assertTrue(EvNluEngine.parse("what's on my calender") is EvIntent.GetCalendarEvents)
        assertTrue(EvNluEngine.parse("show my calendar") is EvIntent.GetCalendarEvents)
        assertTrue(EvNluEngine.parse("check my schedule") is EvIntent.GetCalendarEvents)
    }

    @Test
    fun creatingCalendarEventsIsNotConfusedWithViewing() {
        assertTrue(EvNluEngine.parse("add calendar event meeting at 5 pm") is EvIntent.AddCalendarEvent)
    }

    // ── Notes vs reminders ──────────────────────────────────────────────────
    @Test
    fun deleteNoteIsNotDeleteReminder() {
        assertTrue(EvNluEngine.parse("delete note") is EvIntent.DeleteNote)
        assertTrue(EvNluEngine.parse("remove note") is EvIntent.DeleteNote)
        assertTrue(EvNluEngine.parse("delete all notes") is EvIntent.DeleteNote)
    }

    @Test
    fun deleteReminderStillTargetsReminders() {
        assertTrue(EvNluEngine.parse("delete reminder") is EvIntent.DeleteReminder)
        assertTrue(EvNluEngine.parse("delete all reminders") is EvIntent.DeleteReminder)
    }

    @Test
    fun noteAndReminderCreationStillWorks() {
        assertTrue(EvNluEngine.parse("add note buy milk") is EvIntent.CreateNote)
        assertTrue(EvNluEngine.parse("remind me to call mom at 5 pm") is EvIntent.CreateReminder)
    }

    // ── Network info ────────────────────────────────────────────────────────
    @Test
    fun networkInfoIsRecognised() {
        assertTrue(EvNluEngine.parse("network info") is EvIntent.GetNetworkInfo)
        assertTrue(EvNluEngine.parse("network status") is EvIntent.GetNetworkInfo)
        assertTrue(EvNluEngine.parse("internet info") is EvIntent.GetNetworkInfo)
        assertTrue(EvNluEngine.parse("connection info") is EvIntent.GetNetworkInfo)
        assertTrue(EvNluEngine.parse("my ip") is EvIntent.GetNetworkInfo)
        assertTrue(EvNluEngine.parse("ip address") is EvIntent.GetNetworkInfo)
        assertTrue(EvNluEngine.parse("wifi info") is EvIntent.GetNetworkInfo)
    }

    @Test
    fun wifiAndBluetoothStatusStillWork() {
        assertTrue(EvNluEngine.parse("wifi status") is EvIntent.GetConnectivityStatus)
        assertTrue(EvNluEngine.parse("bluetooth status") is EvIntent.GetConnectivityStatus)
        assertTrue(EvNluEngine.parse("turn on wifi") is EvIntent.ToggleConnectivity)
    }

    // ── Play music ──────────────────────────────────────────────────────────
    @Test
    fun playSongNameBecomesPlayMusic() {
        val intent = EvNluEngine.parse("play believer")
        assertTrue(intent is EvIntent.PlayMusic)
        assertEquals("believer", (intent as EvIntent.PlayMusic).query)

        val artist = EvNluEngine.parse("play believer by imagine dragons")
        assertTrue(artist is EvIntent.PlayMusic)
        assertEquals("believer by imagine dragons", (artist as EvIntent.PlayMusic).query)
    }

    @Test
    fun playMusicGenericStillWorks() {
        assertTrue(EvNluEngine.parse("play music") is EvIntent.PlayMusic)
        assertTrue(EvNluEngine.parse("play song believer") is EvIntent.PlayMusic)
    }

    // ── Invalid commands → plain Unknown (no suggestion lecture) ────────────
    @Test
    fun gibberishIsUnknown() {
        assertTrue(EvNluEngine.parse("do something unusual and random") is EvIntent.Unknown)
        val intent = EvNluEngine.parse("florb the gunk")
        assertTrue(intent is EvIntent.Unknown)
        // The knowledge fallback must NOT swallow genuine nonsense single words.
        assertTrue(EvNluEngine.parse("blargh") is EvIntent.Unknown)
    }

    // ── Existing behaviour must not regress ─────────────────────────────────
    @Test
    fun capabilitiesStillReturnConversation() {
        assertTrue(EvNluEngine.parse("what can you do") is EvIntent.Conversation)
        val help = EvNluEngine.parse("help me")
        assertTrue(help is EvIntent.Help || help is EvIntent.Conversation)
    }

    @Test
    fun timeDateAndBatteryStillWork() {
        assertTrue(EvNluEngine.parse("what time is it") is EvIntent.GetTime)
        assertTrue(EvNluEngine.parse("what day is it") is EvIntent.GetDate)
        assertTrue(EvNluEngine.parse("battery status") is EvIntent.GetBatteryStatus)
        assertTrue(EvNluEngine.parse("weather today") is EvIntent.GetWeather)
    }

    @Test
    fun calculationsStillWork() {
        assertTrue(EvNluEngine.parse("what is 5 plus 5") is EvIntent.Calculate)
        assertTrue(EvNluEngine.parse("15 times 4") is EvIntent.Calculate)
        assertTrue(EvNluEngine.parse("calculate 8 times 7") is EvIntent.Calculate)
    }
}
