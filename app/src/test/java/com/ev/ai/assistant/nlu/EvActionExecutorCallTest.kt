package com.ev.ai.assistant.nlu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Regression tests for the "call mom dials a wrong number" bug.
 *
 * The faulty path fed the raw spoken name straight into a `tel:` URI and matched
 * contacts with an unordered `LIKE '%name%'`. These tests pin down the two guards
 * that fix it: a name must never be treated as a phone number, and contact matches
 * must be ranked so an exact name wins over names that merely contain it.
 */
class EvActionExecutorCallTest {

    // ════════════════════════════════════════════════════════════════════════════
    // Names must never be converted into numbers
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    fun contactNamesAreNotPhoneNumbers() {
        assertNull(EvActionExecutor.normalizePhoneNumber("mom"))
        assertNull(EvActionExecutor.normalizePhoneNumber("Mom"))
        assertNull(EvActionExecutor.normalizePhoneNumber("call mom"))
        assertNull(EvActionExecutor.normalizePhoneNumber("Ramon"))
        assertNull(EvActionExecutor.normalizePhoneNumber("mommy"))
    }

    @Test
    fun spokenNumbersAreDialledVerbatim() {
        assertEquals("5551234", EvActionExecutor.normalizePhoneNumber("5551234"))
        assertEquals("5551234", EvActionExecutor.normalizePhoneNumber("dial 5551234"))
        assertEquals("5551234567", EvActionExecutor.normalizePhoneNumber("call 555 123 4567"))
        assertEquals("+15551234567", EvActionExecutor.normalizePhoneNumber("+1 (555) 123-4567"))
        assertEquals("911", EvActionExecutor.normalizePhoneNumber("911"))
    }

    @Test
    fun tooShortOrBlankInputIsRejected() {
        assertNull(EvActionExecutor.normalizePhoneNumber(""))
        assertNull(EvActionExecutor.normalizePhoneNumber("   "))
        assertNull(EvActionExecutor.normalizePhoneNumber("12"))
        assertNull(EvActionExecutor.normalizePhoneNumber("dial"))
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Contact ranking: exact > prefix > substring > none
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    fun exactNameOutranksSubstringMatches() {
        // Query "mom"
        assertEquals(3, EvActionExecutor.contactMatchScore("Mom", "mom"))
        assertEquals(3, EvActionExecutor.contactMatchScore("MOM", "mom"))
        assertEquals(3, EvActionExecutor.contactMatchScore("Mom ", "mom"))
        assertEquals(2, EvActionExecutor.contactMatchScore("Mommy", "mom"))

        // Query "ann": substring match that is not a prefix must not tie with exact.
        assertEquals(3, EvActionExecutor.contactMatchScore("Ann", "ann"))
        assertEquals(2, EvActionExecutor.contactMatchScore("Anna", "ann"))
        assertEquals(1, EvActionExecutor.contactMatchScore("Joanne", "ann"))

        // Unrelated names score zero.
        assertEquals(0, EvActionExecutor.contactMatchScore("Ramon", "mom"))
        assertEquals(0, EvActionExecutor.contactMatchScore("Mina", "mom"))
        assertEquals(0, EvActionExecutor.contactMatchScore("Bob", "ann"))
    }
}
