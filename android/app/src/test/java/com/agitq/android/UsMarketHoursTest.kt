package com.agitq.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class UsMarketHoursTest {
    private val newYork = ZoneId.of("America/New_York")

    @Test
    fun regularSessionUsesInclusiveOpenAndExclusiveClose() {
        assertFalse(UsMarketHours.isRegularSessionNow(at(2026, 9, 1, 9, 29)))
        assertTrue(UsMarketHours.isRegularSessionNow(at(2026, 9, 1, 9, 30)))
        assertTrue(UsMarketHours.isRegularSessionNow(at(2026, 9, 1, 15, 59)))
        assertFalse(UsMarketHours.isRegularSessionNow(at(2026, 9, 1, 16, 0)))
    }

    @Test
    fun weekendsAndRecurringHolidaysAreClosed() {
        assertFalse(UsMarketHours.isRegularSessionNow(at(2026, 9, 5, 12, 0)))
        assertFalse(UsMarketHours.isRegularSessionNow(at(2026, 1, 19, 12, 0)))
        assertFalse(UsMarketHours.isRegularSessionNow(at(2026, 12, 25, 12, 0)))
    }

    @Test
    fun inputInstantIsConvertedToNewYorkTime() {
        val utc = ZonedDateTime.of(2026, 9, 1, 13, 30, 0, 0, ZoneId.of("UTC"))
        assertTrue(UsMarketHours.isRegularSessionNow(utc))
    }

    @Test
    fun finalCloseSyncRunsOnceBetweenCloseAndFivePm() {
        val tradingDate = at(2026, 9, 1, 16, 0).toLocalDate()

        val atClose = UsMarketHours.automaticRefreshDecision(null, at(2026, 9, 1, 16, 0))
        assertEquals(UsMarketHours.AutomaticRefreshReason.FINAL_CLOSE_SYNC, atClose?.reason)
        assertEquals(tradingDate, atClose?.tradingDate)

        val beforeEnd = UsMarketHours.automaticRefreshDecision(null, at(2026, 9, 1, 16, 59))
        assertEquals(UsMarketHours.AutomaticRefreshReason.FINAL_CLOSE_SYNC, beforeEnd?.reason)

        val duringSession = UsMarketHours.automaticRefreshDecision(tradingDate, at(2026, 9, 1, 15, 30))
        assertEquals(UsMarketHours.AutomaticRefreshReason.REGULAR_SESSION, duringSession?.reason)

        assertNull(UsMarketHours.automaticRefreshDecision(null, at(2026, 9, 1, 17, 0)))
        assertNull(UsMarketHours.automaticRefreshDecision(tradingDate, at(2026, 9, 1, 16, 30)))
    }

    @Test
    fun aPreviousTradingDaysCloseMarkerDoesNotBlockToday() {
        val previousDate = at(2026, 8, 31, 16, 30).toLocalDate()
        val decision = UsMarketHours.automaticRefreshDecision(
            previousDate,
            at(2026, 9, 1, 16, 30)
        )

        assertEquals(UsMarketHours.AutomaticRefreshReason.FINAL_CLOSE_SYNC, decision?.reason)
    }

    @Test
    fun holidaysNeverEnterTheFinalCloseSyncWindow() {
        assertNull(UsMarketHours.automaticRefreshDecision(null, at(2026, 12, 25, 16, 30)))
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int) =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, newYork)
}
