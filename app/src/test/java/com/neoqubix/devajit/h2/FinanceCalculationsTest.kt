package com.neoqubix.devajit.h2

import com.neoqubix.devajit.h2.domain.model.Cart
import com.neoqubix.devajit.h2.domain.model.DateFilter
import com.neoqubix.devajit.h2.domain.model.DateRange
import com.neoqubix.devajit.h2.domain.model.FinancialSummary
import com.neoqubix.devajit.h2.domain.model.ReportType
import com.neoqubix.devajit.h2.domain.model.Transaction
import com.neoqubix.devajit.h2.domain.model.TransactionType
import com.neoqubix.devajit.h2.domain.usecase.ReportCalculator
import com.neoqubix.devajit.h2.utils.formatRupees
import com.neoqubix.devajit.h2.utils.parseAmount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class FinanceCalculationsTest {

    private val zone = ZoneId.of("Asia/Kolkata")

    private fun at(date: String, hour: Int = 12) =
        LocalDate.parse(date).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    private fun sale(cart: String, amount: Double, date: String) =
        Transaction("s$amount$date$cart", TransactionType.REVENUE, cart, amount, at(date))

    private fun expense(cart: String, amount: Double, date: String, category: String = "Gas") =
        Transaction("e$amount$date$cart", TransactionType.EXPENSE, cart, amount, at(date), category = category)

    @Test
    fun profitIsRevenueMinusExpenses() {
        val summary = FinancialSummary.of(listOf(sale("c1", 15000.0, "2026-10-04"), expense("c1", 8000.0, "2026-10-04")))
        assertEquals(15000.0, summary.revenue, 0.0)
        assertEquals(8000.0, summary.expenses, 0.0)
        assertEquals(7000.0, summary.profit, 0.0)
        assertFalse(summary.isLoss)
    }

    @Test
    fun moreExpensesThanRevenueIsALoss() {
        val summary = FinancialSummary.of(listOf(sale("c1", 5000.0, "2026-10-04"), expense("c1", 8000.0, "2026-10-04")))
        assertEquals(-3000.0, summary.profit, 0.0)
        assertTrue(summary.isLoss)
    }

    @Test
    fun dailyReportGroupsByDayNewestFirst() {
        val rows = ReportCalculator.report(
            ReportType.DAILY,
            listOf(
                sale("c1", 100.0, "2026-10-01"),
                sale("c1", 200.0, "2026-10-02"),
                expense("c1", 50.0, "2026-10-02")
            ),
            zone = zone
        )
        assertEquals(2, rows.size)
        assertEquals(150.0, rows[0].summary.profit, 0.0)
        assertEquals(100.0, rows[1].summary.profit, 0.0)
    }

    @Test
    fun cartWiseReportSeparatesCartsAndSortsByProfit() {
        val carts = listOf(Cart("c1", "Burger Cart"), Cart("c2", "Momo Cart"))
        val rows = ReportCalculator.report(
            ReportType.CART_WISE,
            listOf(sale("c1", 1000.0, "2026-10-01"), sale("c2", 3000.0, "2026-10-01"), expense("c2", 500.0, "2026-10-01")),
            carts,
            zone
        )
        assertEquals(listOf("Momo Cart", "Burger Cart"), rows.map { it.label })
        assertEquals(2500.0, rows[0].summary.profit, 0.0)
        assertEquals(1000.0, rows[1].summary.profit, 0.0)
    }

    @Test
    fun expenseBreakdownSumsCategories() {
        val items = ReportCalculator.expenseBreakdown(
            listOf(expense("c1", 800.0, "2026-10-04", "Gas"), expense("c1", 200.0, "2026-10-04", "Gas"), expense("c1", 2500.0, "2026-10-04", "Raw Materials"))
        )
        assertEquals("Raw Materials", items[0].category)
        assertEquals(1000.0, items.first { it.category == "Gas" }.amount, 0.0)
    }

    @Test
    fun trendFillsEmptyDays() {
        val trend = ReportCalculator.trend(listOf(sale("c1", 100.0, "2026-10-03")), LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-04"), zone)
        assertEquals(4, trend.size)
        assertEquals(100.0, trend[2].summary.revenue, 0.0)
    }

    @Test
    fun dateFiltersCoverTheRightDays() {
        val today = LocalDate.parse("2026-10-04") // a Sunday
        val week = DateRange.forFilter(DateFilter.THIS_WEEK, today = today, zone = zone)
        assertEquals(LocalDate.parse("2026-09-28"), week.startDate(zone))
        assertEquals(today, week.endDate(zone))
        val yesterday = DateRange.forFilter(DateFilter.YESTERDAY, today = today, zone = zone)
        assertTrue(yesterday.contains(at("2026-10-03", 23)))
        assertFalse(yesterday.contains(at("2026-10-04", 0)))
        assertEquals(LocalDate.parse("2026-10-01"), DateRange.forFilter(DateFilter.THIS_MONTH, today = today, zone = zone).startDate(zone))
    }

    @Test
    fun amountsMustBePositive() {
        assertNull(parseAmount("0"))
        assertNull(parseAmount("-5"))
        assertNull(parseAmount("abc"))
        assertEquals(1500.5, parseAmount("1500.50")!!, 0.0)
    }

    @Test
    fun rupeesUseIndianGrouping() {
        assertEquals("₹2,50,000", formatRupees(250000.0))
        assertEquals("₹15,000", formatRupees(15000.0))
        assertEquals("₹1,234.50", formatRupees(1234.5))
    }
}
