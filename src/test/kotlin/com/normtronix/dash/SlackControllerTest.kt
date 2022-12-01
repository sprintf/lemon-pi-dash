package com.normtronix.dash

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class SlackControllerTest {

    @Test
    fun testGetTimeInSecs() {
        val s = SlackController()
        assertEquals(115, s.getTimeInSecs("1:55"))
        assertEquals(60, s.getTimeInSecs("1:0"))
        assertEquals(60, s.getTimeInSecs("1:00"))
        assertEquals(60, s.getTimeInSecs("0:60"))
        assertEquals(61, s.getTimeInSecs("1:1"))
        assertEquals(61, s.getTimeInSecs("1:01"))
    }
}