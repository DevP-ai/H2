package com.neoqubix.devajit.h2.domain.model

// Totals are always calculated from the sales and expenses themselves; profit is never stored
data class FinancialSummary(
    val revenue: Double = 0.0,
    val expenses: Double = 0.0
) {
    val profit: Double get() = revenue - expenses
    val isLoss: Boolean get() = profit < 0

    companion object {
        fun of(transactions: List<Transaction>): FinancialSummary = FinancialSummary(
            revenue = transactions.filter { it.isRevenue }.sumOf { it.amount },
            expenses = transactions.filter { !it.isRevenue }.sumOf { it.amount }
        )
    }
}

data class CategoryTotal(val category: String, val amount: Double)

data class CartPerformance(val cart: Cart, val summary: FinancialSummary)

// One row of a report: a day, week, month, year or cart
data class ReportRow(
    val key: String,
    val label: String,
    val summary: FinancialSummary,
    val sortKey: Long = 0L
)

enum class ReportType(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly"),
    CART_WISE("Cart-wise"),
    OVERALL("Overall")
}
