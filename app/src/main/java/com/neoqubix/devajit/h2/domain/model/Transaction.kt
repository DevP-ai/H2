package com.neoqubix.devajit.h2.domain.model

enum class TransactionType { REVENUE, EXPENSE }

// A sale (revenue) or an expense. Dates are epoch millis; category is only set on expenses.
data class Transaction(
    val id: String,
    val type: TransactionType,
    val cartId: String,
    val amount: Double,
    val date: Long,
    val description: String = "",
    val category: String? = null,
    val createdBy: String = "",
    val createdAt: Long? = null,
    // Set when an admin corrected the record
    val updatedBy: String? = null,
    val updatedAt: Long? = null,
    // True while the write is still waiting to reach the server (offline)
    val pendingSync: Boolean = false
) {
    val isRevenue: Boolean get() = type == TransactionType.REVENUE
    val isEdited: Boolean get() = updatedBy != null
    val title: String get() = if (isRevenue) description.ifBlank { "Revenue" } else category ?: "Expense"
}

data class NewRevenue(
    val cartId: String,
    val amount: Double,
    val date: Long,
    val description: String
)

// An admin's correction of an existing record; the cart and creator never change
data class TransactionEdit(
    val amount: Double,
    val date: Long,
    val description: String,
    // Expenses only
    val category: String? = null
)

data class NewExpense(
    val cartId: String,
    val category: String,
    val amount: Double,
    val date: Long,
    val description: String
)

// The predefined categories; add new ones here
object ExpenseCategories {
    val all = listOf(
        "Raw Materials",
        "Vegetables",
        "Meat",
        "Oil",
        "Gas",
        "Electricity",
        "Packaging",
        "Transportation",
        "Staff Salary",
        "Maintenance",
        "Rent",
        "Marketing",
        "Miscellaneous"
    )
}
