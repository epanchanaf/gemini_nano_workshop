package com.sdkecuador.gemininano

import org.junit.Assert.assertEquals
import org.junit.Test

class UtilsTest {
    @Test
    fun bytesToMB_returnsZeroForNegativeValues() {
        assertEquals(0.0, (-1L).bytesToMB(), 0.0)
    }

    @Test
    fun bytesToMB_convertsAndRoundsToTwoDecimalPlaces() {
        assertEquals(1.0, 1_048_576L.bytesToMB(), 0.0)
        assertEquals(1.5, 1_572_864L.bytesToMB(), 0.0)
        assertEquals(1.23, 1_290_000L.bytesToMB(), 0.0)
    }
}
