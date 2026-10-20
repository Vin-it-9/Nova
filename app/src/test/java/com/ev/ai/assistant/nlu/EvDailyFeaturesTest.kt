package com.ev.ai.assistant.nlu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the everyday "daily driver" features:
 *  - quick speed-dial contacts (call dad / mom / shruti didi / bro / jiju)
 *  - navigation ("locate me to satara", "take me to pune")
 *  - alarm time parsing ("7 30 pm", "quarter past seven", "7 in the morning")
 */
class EvDailyFeaturesTest {

    // ════════════════════════════════════════════════════════════════════════════
    // Quick contacts: the hardcoded relationships must win over fuzzy matching
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    fun momAndDadResolveToTheirExactNumbers() {
        assertEquals("+919623458823", EvQuickContacts.resolve("mom")?.number)
        assertEquals("+919623458894", EvQuickContacts.resolve("dad")?.number)
        assertEquals("Mom", EvQuickContacts.resolve("mom")?.name)
        assertEquals("Dad", EvQuickContacts.resolve("dad")?.name)
    }

    @Test
    fun momAndDadMarathiAndCommonVariantsResolve() {
        // Marathi + common substitutes people actually use.
        for (alias in listOf("mummy", "mumma", "maa", "aai", "amma")) {
            assertEquals("Expected $alias -> Mom", "+919623458823", EvQuickContacts.resolve(alias)?.number)
        }
        for (alias in listOf("papa", "pappa", "baba", "daddy", "appa")) {
            assertEquals("Expected $alias -> Dad", "+919623458894", EvQuickContacts.resolve(alias)?.number)
        }
    }

    @Test
    fun sisterAndShrutiVariantsResolveToShrutiDidi() {
        assertEquals("+919623440700", EvQuickContacts.resolve("sister")?.number)
        assertEquals("+919623440700", EvQuickContacts.resolve("didi")?.number)
        assertEquals("+919623440700", EvQuickContacts.resolve("shruti")?.number)
        // Marathi "tai" (elder sister) and the multi-word phrase "shruti didi".
        assertEquals("+919623440700", EvQuickContacts.resolve("tai")?.number)
        assertEquals("+919623440700", EvQuickContacts.resolve("shruti didi")?.number)
    }

    @Test
    fun pushkarVariantsResolveToPushkar() {
        assertEquals("+918956176605", EvQuickContacts.resolve("pushkar")?.number)
        assertEquals("+918956176605", EvQuickContacts.resolve("bro")?.number)
        assertEquals("+918956176605", EvQuickContacts.resolve("brother")?.number)
        assertEquals("+918956176605", EvQuickContacts.resolve("friend")?.number)
        assertEquals("+918956176605", EvQuickContacts.resolve("virat")?.number)
        assertEquals("+918956176605", EvQuickContacts.resolve("bhai")?.number)
    }

    @Test
    fun rushikeshVariantsResolveToRushikeshPhalke() {
        assertEquals("+917775910607", EvQuickContacts.resolve("rushikesh")?.number)
        assertEquals("+917775910607", EvQuickContacts.resolve("rushi")?.number)
        // How he's actually saved on the phone.
        assertEquals("+917775910607", EvQuickContacts.resolve("rushiii")?.number)
        assertEquals("+917775910607", EvQuickContacts.resolve("daji")?.number)
        assertEquals("+917775910607", EvQuickContacts.resolve("jiju")?.number)
    }

    @Test
    fun fillerWordsAreStrippedBeforeLookup() {
        assertEquals("+919623458894", EvQuickContacts.resolve("my dad")?.number)
        assertEquals("+919623458823", EvQuickContacts.resolve("call mummy")?.number)
        assertEquals("+919623458894", EvQuickContacts.resolve("phone papa ji")?.number)
        assertEquals("+919623440700", EvQuickContacts.resolve("call my sister")?.number)
    }

    @Test
    fun unrelatedOrUnknownNamesFallThroughToAddressBook() {
        // A real, different contact name must NOT be hijacked by the quick layer.
        assertNull(EvQuickContacts.resolve("ananya"))
        assertNull(EvQuickContacts.resolve("friend ananya"))
        assertNull(EvQuickContacts.resolve("omkar"))
        assertNull(EvQuickContacts.resolve(""))
        assertNull(EvQuickContacts.resolve(null))
        assertNull(EvQuickContacts.resolve("please"))
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Navigation: "locate me to satara" and friends must open directions
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    fun locateMeToCityOpensDirections() {
        val intent = EvNluEngine.parse("locate me to satara")
        assertTrue("Expected GetDirections, got $intent", intent is EvIntent.GetDirections)
        assertEquals("satara", (intent as EvIntent.GetDirections).destination)
    }

    @Test
    fun navigationPhrasingVariantsAllWork() {
        val queries = mapOf(
            "take me to pune" to "pune",
            "navigate to mumbai" to "mumbai",
            "directions to the airport" to "the airport",
            "route to home" to "home",
            "how do i get to college" to "college",
            "show me the way to delhi" to "delhi"
        )
        for ((q, dest) in queries) {
            val intent = EvNluEngine.parse(q)
            assertTrue("Expected GetDirections for \"$q\", got $intent", intent is EvIntent.GetDirections)
            assertEquals("Wrong destination for \"$q\"", dest, (intent as EvIntent.GetDirections).destination)
        }
    }

    @Test
    fun bareLocateMeOpensMapsWithoutDestination() {
        val intent = EvNluEngine.parse("locate me")
        assertTrue("Expected OpenMaps for \"locate me\", got $intent", intent is EvIntent.OpenMaps)
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Alarms: flexible spoken time parsing
    // ════════════════════════════════════════════════════════════════════════════

    private fun alarmHourMinute(query: String): Pair<Int, Int>? {
        val intent = EvNluEngine.parse(query)
        assertTrue("Expected SetAlarm for \"$query\", got $intent", intent is EvIntent.SetAlarm)
        return (intent as EvIntent.SetAlarm).hour to intent.minute
    }

    @Test
    fun simpleAlarmTimes() {
        assertEquals(7 to 0, alarmHourMinute("set alarm for 7"))
        assertEquals(7 to 30, alarmHourMinute("set alarm for 7:30"))
        assertEquals(19 to 0, alarmHourMinute("set alarm for 7 pm"))
        assertEquals(7 to 0, alarmHourMinute("set alarm for 7 am"))
        assertEquals(12 to 0, alarmHourMinute("set alarm for 12"))
    }

    @Test
    fun spacedTimeWithAmPm() {
        // Previously broken: "7 30 pm" fell through and only the hour survived.
        assertEquals(19 to 30, alarmHourMinute("set alarm for 7 30 pm"))
        assertEquals(7 to 30, alarmHourMinute("set alarm for 7 30 am"))
        assertEquals(19 to 30, alarmHourMinute("wake me up at 7 30 in the evening"))
    }

    @Test
    fun dayPartPhrasesInferAmPm() {
        assertEquals(7 to 0, alarmHourMinute("wake me up at 7 in the morning"))
        assertEquals(20 to 0, alarmHourMinute("set alarm for 8 in the evening"))
        assertEquals(21 to 0, alarmHourMinute("set alarm for 9 tonight"))
    }

    @Test
    fun wordAndFractionTimes() {
        assertEquals(7 to 15, alarmHourMinute("set alarm for quarter past seven"))
        assertEquals(7 to 30, alarmHourMinute("set alarm for half past 7"))
        assertEquals(12 to 0, alarmHourMinute("set alarm for noon"))
        assertEquals(0 to 0, alarmHourMinute("set alarm for midnight"))
    }

    @Test
    fun alarmDeleteAndEditAreNotMisreadAsSetAlarm() {
        // The flexible time rule must never hijack management commands.
        assertTrue(EvNluEngine.parse("delete alarm") is EvIntent.DeleteAlarm)
        assertTrue(EvNluEngine.parse("cancel alarm") is EvIntent.DeleteAlarm)
        assertTrue(EvNluEngine.parse("edit alarm") is EvIntent.EditAlarm)
        assertTrue(EvNluEngine.parse("snooze") is EvIntent.SnoozeAlarm)
        assertTrue(EvNluEngine.parse("snooze alarm") is EvIntent.SnoozeAlarm)
        assertTrue(EvNluEngine.parse("check alarm") is EvIntent.CheckAlarm)
        // "change alarm from 7 to 8 am" -> EditAlarm, not SetAlarm.
        assertTrue(EvNluEngine.parse("change alarm from 7 to 8 am") is EvIntent.EditAlarm)
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Standalone time parser (used by both deterministic and classifier paths)
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    fun spokenTimeParserHandlesMixedShapes() {
        assertEquals(19 to 45, EvNluEngine.parseSpokenTime("7 45 pm"))
        assertEquals(6 to 5, EvNluEngine.parseSpokenTime("06:05"))
        assertEquals(23 to 10, EvNluEngine.parseSpokenTime("11:10 pm"))
        assertEquals(13 to 0, EvNluEngine.parseSpokenTime("1 pm"))
        // Returns null when no time is present at all.
        assertNull(EvNluEngine.parseSpokenTime("turn on bluetooth"))
    }
}
