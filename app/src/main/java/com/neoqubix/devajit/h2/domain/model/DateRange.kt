package com.neoqubix.devajit.h2.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class DateFilter(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    THIS_YEAR("This Year"),
    CUSTOM("Custom")
}

// Half-open range [start, endExclusive) in epoch millis, local time
data class DateRange(val start: Long, val endExclusive: Long) {

    fun contains(millis: Long): Boolean = millis in start until endExclusive

    fun startDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate = Instant.ofEpochMilli(start).atZone(zone).toLocalDate()

    // Last day included in the range
    fun endDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate = Instant.ofEpochMilli(endExclusive - 1).atZone(zone).toLocalDate()

    val dayCount: Long
        get() = java.time.temporal.ChronoUnit.DAYS.between(startDate(), endDate()) + 1

    fun label(): String {
        val f = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
        val s = startDate()
        val e = endDate()
        return if (s == e) s.format(f) else "${s.format(f)} – ${e.format(f)}"
    }

    companion object {
        fun ofDates(from: LocalDate, toInclusive: LocalDate, zone: ZoneId = ZoneId.systemDefault()): DateRange {
            val (a, b) = if (from <= toInclusive) from to toInclusive else toInclusive to from
            return DateRange(
                a.atStartOfDay(zone).toInstant().toEpochMilli(),
                b.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            )
        }

        // The range for a preset filter; CUSTOM uses the given custom range (today if none)
        fun forFilter(
            filter: DateFilter,
            custom: DateRange? = null,
            today: LocalDate = LocalDate.now(),
            zone: ZoneId = ZoneId.systemDefault()
        ): DateRange = when (filter) {
            DateFilter.TODAY -> ofDates(today, today, zone)
            DateFilter.YESTERDAY -> today.minusDays(1).let { ofDates(it, it, zone) }
            DateFilter.THIS_WEEK -> ofDates(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), today, zone)
            DateFilter.THIS_MONTH -> ofDates(today.withDayOfMonth(1), today, zone)
            DateFilter.THIS_YEAR -> ofDates(today.withDayOfYear(1), today, zone)
            DateFilter.CUSTOM -> custom ?: ofDates(today, today, zone)
        }
    }
}

data class DateSelection(
    val filter: DateFilter = DateFilter.TODAY,
    val custom: DateRange? = null
) {
    val range: DateRange get() = DateRange.forFilter(filter, custom)
}
