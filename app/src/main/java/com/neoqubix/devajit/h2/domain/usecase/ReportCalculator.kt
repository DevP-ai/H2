package com.neoqubix.devajit.h2.domain.usecase

import com.neoqubix.devajit.h2.domain.model.Cart
import com.neoqubix.devajit.h2.domain.model.CartPerformance
import com.neoqubix.devajit.h2.domain.model.CategoryTotal
import com.neoqubix.devajit.h2.domain.model.FinancialSummary
import com.neoqubix.devajit.h2.domain.model.ReportRow
import com.neoqubix.devajit.h2.domain.model.ReportType
import com.neoqubix.devajit.h2.domain.model.Transaction
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/*
 * Every total in the app comes from here: revenue = sum of sales, expenses = sum of expenses,
 * profit/loss = revenue - expenses, for any group of transactions (day, week, month, year, cart, all).
 */
object ReportCalculator {

    fun expenseBreakdown(transactions: List<Transaction>): List<CategoryTotal> =
        transactions.filter { !it.isRevenue }
            .groupBy { it.category ?: "Miscellaneous" }
            .map { (category, items) -> CategoryTotal(category, items.sumOf { it.amount }) }
            .sortedByDescending { it.amount }

    fun cartPerformance(carts: List<Cart>, transactions: List<Transaction>): List<CartPerformance> {
        val byCart = transactions.groupBy { it.cartId }
        return carts.map { CartPerformance(it, FinancialSummary.of(byCart[it.id].orEmpty())) }
            .sortedByDescending { it.summary.profit }
    }

    fun report(
        type: ReportType,
        transactions: List<Transaction>,
        carts: List<Cart> = emptyList(),
        zone: ZoneId = ZoneId.systemDefault()
    ): List<ReportRow> = when (type) {
        ReportType.DAILY -> byPeriod(transactions, zone, { it }, DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault()))
        ReportType.WEEKLY -> byPeriod(
            transactions, zone,
            { it.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) },
            null
        )
        ReportType.MONTHLY -> byPeriod(transactions, zone, { it.withDayOfMonth(1) }, DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()))
        ReportType.YEARLY -> byPeriod(transactions, zone, { it.withDayOfYear(1) }, DateTimeFormatter.ofPattern("yyyy", Locale.getDefault()))
        ReportType.CART_WISE -> cartPerformance(carts, transactions).map {
            ReportRow(it.cart.id, it.cart.name, it.summary)
        } + unknownCartRow(carts, transactions)
        ReportType.OVERALL -> listOf(ReportRow("all", "All carts", FinancialSummary.of(transactions)))
    }

    // Records whose cart isn't in the list (e.g. a cart that was removed) are still counted
    private fun unknownCartRow(carts: List<Cart>, transactions: List<Transaction>): List<ReportRow> {
        val known = carts.map { it.id }.toSet()
        val others = transactions.filter { it.cartId !in known }
        return if (others.isEmpty()) emptyList() else listOf(ReportRow("other", "Other carts", FinancialSummary.of(others)))
    }

    // Newest period first
    private fun byPeriod(
        transactions: List<Transaction>,
        zone: ZoneId,
        periodStart: (LocalDate) -> LocalDate,
        format: DateTimeFormatter?
    ): List<ReportRow> {
        val weekFormat = DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())
        return transactions
            .groupBy { periodStart(Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate()) }
            .map { (start, items) ->
                val label = format?.let { start.format(it) } ?: "${start.format(weekFormat)} – ${start.plusDays(6).format(weekFormat)}"
                ReportRow(start.toString(), label, FinancialSummary.of(items), start.toEpochDay())
            }
            .sortedByDescending { it.sortKey }
    }

    // Chart points oldest first: one per day for ranges up to 31 days, otherwise one per month.
    // Days without records are included so the chart has no gaps.
    fun trend(transactions: List<Transaction>, from: LocalDate, to: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<ReportRow> {
        val days = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1
        return if (days <= 31) {
            val byDay = transactions.groupBy { Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate() }
            val f = DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())
            (0 until days).map { i ->
                val d = from.plusDays(i)
                ReportRow(d.toString(), d.format(f), FinancialSummary.of(byDay[d].orEmpty()), d.toEpochDay())
            }
        } else {
            val byMonth = transactions.groupBy { Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate().withDayOfMonth(1) }
            val f = DateTimeFormatter.ofPattern("MMM yy", Locale.getDefault())
            generateSequence(from.withDayOfMonth(1)) { it.plusMonths(1) }
                .takeWhile { !it.isAfter(to) }
                .map { m -> ReportRow(m.toString(), m.format(f), FinancialSummary.of(byMonth[m].orEmpty()), m.toEpochDay()) }
                .toList()
        }
    }
}
