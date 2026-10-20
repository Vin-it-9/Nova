package com.ev.ai.assistant.nlu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Regression tests for unit conversion, which used to be unimplemented. */
class EvActionExecutorConvertTest {

    private fun assertClose(expected: Double, actual: Double?) {
        assertEquals(expected, actual ?: Double.NaN, 0.01)
    }

    @Test
    fun lengthConversions() {
        assertClose(3.10686, EvActionExecutor.convertUnit(5.0, "km", "miles"))
        assertClose(5000.0, EvActionExecutor.convertUnit(5.0, "km", "m"))
        assertClose(12.0, EvActionExecutor.convertUnit(1.0, "ft", "inches"))
        assertClose(100.0, EvActionExecutor.convertUnit(1.0, "m", "cm"))
    }

    @Test
    fun massConversions() {
        assertClose(2.20462, EvActionExecutor.convertUnit(1.0, "kg", "pounds"))
        assertClose(1000.0, EvActionExecutor.convertUnit(1.0, "kg", "g"))
    }

    @Test
    fun temperatureConversions() {
        assertClose(32.0, EvActionExecutor.convertUnit(0.0, "celsius", "fahrenheit"))
        assertClose(100.0, EvActionExecutor.convertUnit(212.0, "fahrenheit", "celsius"))
        assertClose(273.15, EvActionExecutor.convertUnit(0.0, "celsius", "kelvin"))
    }

    @Test
    fun volumeConversions() {
        assertClose(1000.0, EvActionExecutor.convertUnit(1.0, "l", "ml"))
        assertClose(3.78541, EvActionExecutor.convertUnit(1.0, "gallons", "litres"))
    }

    @Test
    fun incompatibleDimensionsReturnNull() {
        assertNull(EvActionExecutor.convertUnit(1.0, "km", "kg"))
        assertNull(EvActionExecutor.convertUnit(1.0, "celsius", "miles"))
        assertNull(EvActionExecutor.convertUnit(1.0, "furlongs", "miles"))
    }

    @Test
    fun sameUnitIsIdentity() {
        assertClose(7.0, EvActionExecutor.convertUnit(7.0, "km", "km"))
    }
}
