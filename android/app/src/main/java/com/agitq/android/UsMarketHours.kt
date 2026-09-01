package com.agitq.android

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/**
 * 미국 주식시장(NYSE/Nasdaq 공통) 정규장 자동 갱신용 시간 판정.
 *
 * - 기준 시간대: America/New_York (EST/EDT 자동 반영)
 * - 정규장: 09:30 이상, 16:00 미만
 * - 주말 및 정기 휴장일에는 자동 갱신하지 않음
 * - 임시 휴장 같은 비정기 이벤트는 별도 서버 캘린더가 없는 한 자동 판정할 수 없음
 */
object UsMarketHours {
    private val NEW_YORK: ZoneId = ZoneId.of("America/New_York")
    private val REGULAR_OPEN: LocalTime = LocalTime.of(9, 30)
    private val REGULAR_CLOSE: LocalTime = LocalTime.of(16, 0)

    fun isRegularSessionNow(now: ZonedDateTime = ZonedDateTime.now(NEW_YORK)): Boolean {
        val nyNow = now.withZoneSameInstant(NEW_YORK)
        val date = nyNow.toLocalDate()
        val time = nyNow.toLocalTime()

        if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) {
            return false
        }
        if (isRecurringMarketHoliday(date)) {
            return false
        }

        return !time.isBefore(REGULAR_OPEN) && time.isBefore(REGULAR_CLOSE)
    }

    private fun isRecurringMarketHoliday(date: LocalDate): Boolean {
        val year = date.year

        // New Year's Day: 일요일이면 다음 월요일 휴장. 토요일이면 전날 금요일은 NYSE가 정상 개장한다.
        val newYears = LocalDate.of(year, Month.JANUARY, 1)
        if (date == newYears ||
            (newYears.dayOfWeek == DayOfWeek.SUNDAY && date == newYears.plusDays(1))) {
            return true
        }

        // Martin Luther King Jr. Day: 1월 셋째 월요일
        if (date == LocalDate.of(year, Month.JANUARY, 1)
                .with(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.MONDAY))) {
            return true
        }

        // Presidents' Day: 2월 셋째 월요일
        if (date == LocalDate.of(year, Month.FEBRUARY, 1)
                .with(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.MONDAY))) {
            return true
        }

        // Good Friday
        if (date == easterSunday(year).minusDays(2)) {
            return true
        }

        // Memorial Day: 5월 마지막 월요일
        if (date == LocalDate.of(year, Month.MAY, 1)
                .with(TemporalAdjusters.lastInMonth(DayOfWeek.MONDAY))) {
            return true
        }

        // Juneteenth: 2022년부터 NYSE 정규 휴장일
        if (year >= 2022 && date == observedFixedHoliday(LocalDate.of(year, Month.JUNE, 19))) {
            return true
        }

        // Independence Day
        if (date == observedFixedHoliday(LocalDate.of(year, Month.JULY, 4))) {
            return true
        }

        // Labor Day: 9월 첫째 월요일
        if (date == LocalDate.of(year, Month.SEPTEMBER, 1)
                .with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY))) {
            return true
        }

        // Thanksgiving: 11월 넷째 목요일
        if (date == LocalDate.of(year, Month.NOVEMBER, 1)
                .with(TemporalAdjusters.dayOfWeekInMonth(4, DayOfWeek.THURSDAY))) {
            return true
        }

        // Christmas Day
        if (date == observedFixedHoliday(LocalDate.of(year, Month.DECEMBER, 25))) {
            return true
        }

        return false
    }

    private fun observedFixedHoliday(actual: LocalDate): LocalDate = when (actual.dayOfWeek) {
        DayOfWeek.SATURDAY -> actual.minusDays(1)
        DayOfWeek.SUNDAY -> actual.plusDays(1)
        else -> actual
    }

    /** Gregorian Easter Sunday (Meeus/Jones/Butcher algorithm). */
    private fun easterSunday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(year, month, day)
    }
}
