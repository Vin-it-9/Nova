package com.ev.ai.assistant.nlu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the NLU bugs reported by users. Each case used to mis-parse
 * (often because entity regexes were double-escaped, or because the classifier's
 * `required` set was treated as AND instead of OR).
 */
class EvNluEngineRegressionTest {

    @Test
    fun flashlightIsNotMistakenForBluetooth() {
        assertEquals(EvIntent.ToggleFlashlight(true), EvNluEngine.parse("turn on torch"))
        assertEquals(EvIntent.ToggleFlashlight(true), EvNluEngine.parse("torch on"))
        assertEquals(EvIntent.ToggleFlashlight(true), EvNluEngine.parse("turn on flashlight"))
        assertEquals(EvIntent.ToggleFlashlight(false), EvNluEngine.parse("turn off flashlight"))
    }

    @Test
    fun volumeParsesPercentAndMute() {
        assertEquals(
            EvIntent.SetVolume(VolumeStream.MEDIA, 80, relative = false),
            EvNluEngine.parse("volume to 80%")
        )
        assertEquals(
            EvIntent.SetVolume(VolumeStream.MEDIA, 40, relative = false),
            EvNluEngine.parse("set volume 40")
        )
        assertEquals(
            EvIntent.SetVolume(VolumeStream.MEDIA, 0, relative = false),
            EvNluEngine.parse("mute")
        )
        val up = EvNluEngine.parse("volume up") as EvIntent.SetVolume
        assertTrue("volume up should be relative", up.relative)
    }

    @Test
    fun definitionDoesNotBecomeACalculation() {
        assertEquals(EvIntent.GetDefinition("ephemeral"), EvNluEngine.parse("define ephemeral"))
        assertEquals(EvIntent.GetDefinition("ephemeral"), EvNluEngine.parse("what is the meaning of ephemeral"))
        assertEquals(EvIntent.GetDefinition("ephemeral"), EvNluEngine.parse("what is meaning of ephemeral"))
    }

    @Test
    fun unitConversionExtractsValueAndUnits() {
        assertEquals(
            EvIntent.ConvertUnits(5.0, "km", "miles", null),
            EvNluEngine.parse("convert 5 km to miles")
        )
        assertEquals(
            EvIntent.ConvertUnits(20.0, "celsius", "fahrenheit", null),
            EvNluEngine.parse("convert 20 celsius to fahrenheit")
        )
    }

    @Test
    fun reminderKeepsDueTimeAndCleanTitle() {
        val intent = EvNluEngine.parse("remind me to call mom at 5:00")
        assertEquals(EvIntent.CreateReminder("call mom", "05:00", null, null), intent)
    }

    @Test
    fun directionsDestinationIsNotTheWholePhrase() {
        assertEquals(EvIntent.GetDirections(null, "new york"), EvNluEngine.parse("directions to new york"))
        assertEquals(EvIntent.GetDirections(null, "london"), EvNluEngine.parse("navigate to london"))
    }

    @Test
    fun alarmEditingExtractsBothTimes() {
        assertEquals(
            EvIntent.EditAlarm(7, 0, 8, 0, null),
            EvNluEngine.parse("change alarm from 7 to 8 am")
        )
    }

    @Test
    fun smsAndCallResolveNamesNotWholePhrase() {
        assertEquals(EvIntent.MakeCall("mom", "mom"), EvNluEngine.parse("call mom"))
        assertEquals(EvIntent.SendSms("mom", "hello"), EvNluEngine.parse("send sms to mom hello"))
    }

    @Test
    fun weatherAndStockExtractSubjects() {
        assertEquals(EvIntent.GetWeather("london", false), EvNluEngine.parse("weather in london"))
        assertEquals(EvIntent.GetStockPrice("APPLE"), EvNluEngine.parse("stock price of apple"))
    }
}
