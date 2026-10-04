package com.neoqubix.devajit.h2.utils

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

// Indian digit grouping (last 3 digits, then pairs): 2,50,000 / 1,00,00,000. Done by hand so it never depends on the device locale.
private fun groupIndian(digits: String): String {
    if (digits.length <= 3) return digits
    val last3 = digits.takeLast(3)
    val rest = digits.dropLast(3)
    return rest.reversed().chunked(2).joinToString(",").reversed() + "," + last3
}

// ₹15,000 / ₹2,50,000 / ₹1,234.50
fun formatRupees(amount: Double): String {
    val paise = Math.round(abs(amount) * 100)
    val rupees = groupIndian((paise / 100).toString())
    val fraction = paise % 100
    val text = if (fraction == 0L) rupees else rupees + "." + fraction.toString().padStart(2, '0')
    return (if (amount < 0 && paise != 0L) "-₹" else "₹") + text
}

// Short form for chart labels: ₹15K, ₹2.5L, ₹1.2Cr
fun formatRupeesShort(amount: Double): String {
    val a = abs(amount)
    val sign = if (amount < 0) "-" else ""
    fun trim(v: Double) = if (v % 1.0 == 0.0) v.toLong().toString() else "%.1f".format(Locale.US, v)
    return sign + "₹" + when {
        a >= 1_00_00_000 -> trim(a / 1_00_00_000) + "Cr"
        a >= 1_00_000 -> trim(a / 1_00_000) + "L"
        a >= 1_000 -> trim(a / 1_000) + "K"
        else -> trim(a)
    }
}

private val dateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
private val timeFormat = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())

fun Long.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

fun formatDate(millis: Long): String = millis.toLocalDate().format(dateFormat)

fun formatDate(date: LocalDate): String = date.format(dateFormat)

fun formatTime(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime().format(timeFormat)

fun greetingFor(time: LocalTime = LocalTime.now()): String = when (time.hour) {
    in 5..11 -> "Good Morning"
    in 12..16 -> "Good Afternoon"
    else -> "Good Evening"
}

// The amount typed into a form: positive, at most 2 decimals; null when invalid
fun parseAmount(text: String): Double? {
    val value = text.trim().replace(",", "").toDoubleOrNull() ?: return null
    if (value <= 0 || value.isNaN() || value.isInfinite()) return null
    if (value > 1_00_00_000) return null
    return Math.round(value * 100) / 100.0
}
