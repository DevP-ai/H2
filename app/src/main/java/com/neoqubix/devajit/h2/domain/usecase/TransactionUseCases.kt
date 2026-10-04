package com.neoqubix.devajit.h2.domain.usecase

import com.neoqubix.devajit.h2.domain.model.DateRange
import com.neoqubix.devajit.h2.domain.model.ExpenseCategories
import com.neoqubix.devajit.h2.domain.model.NewExpense
import com.neoqubix.devajit.h2.domain.model.NewRevenue
import com.neoqubix.devajit.h2.domain.model.Role
import com.neoqubix.devajit.h2.domain.model.Transaction
import com.neoqubix.devajit.h2.domain.model.UserProfile
import com.neoqubix.devajit.h2.domain.repository.SaveResult
import com.neoqubix.devajit.h2.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

private const val MAX_DESCRIPTION = 200

class ObserveTransactionsUseCase @Inject constructor(private val repository: TransactionRepository) {
    /*
     * A manager only ever queries their own cart, whatever cartId is asked for; an admin may pass null for every cart.
     * A manager without a cart gets nothing.
     */
    operator fun invoke(user: UserProfile, cartId: String?, range: DateRange, limit: Long? = null): Flow<List<Transaction>> {
        val scope = if (user.role == Role.ADMIN) cartId else user.cartId ?: return flowOf(emptyList())
        return repository.observeTransactions(scope, range, limit)
    }
}

// Checks shared by both forms; the cart always comes from the signed-in manager's profile
private fun validate(user: UserProfile, amount: Double?, date: Long?, description: String): String {
    val cartId = user.cartId ?: throw IllegalStateException("Your account has not been assigned to a food cart yet.")
    require(amount != null && amount > 0) { "Please enter an amount greater than 0." }
    require(date != null) { "Please select a date." }
    require(date <= System.currentTimeMillis() + 24L * 60 * 60 * 1000) { "The date can't be in the future." }
    require(description.trim().length <= MAX_DESCRIPTION) { "Description is too long (max $MAX_DESCRIPTION characters)." }
    return cartId
}

class AddRevenueUseCase @Inject constructor(private val repository: TransactionRepository) {
    suspend operator fun invoke(user: UserProfile, amount: Double?, date: Long?, description: String): Result<SaveResult> {
        val cartId = runCatching { validate(user, amount, date, description) }.getOrElse { return Result.failure(it) }
        return repository.addRevenue(NewRevenue(cartId, amount!!, date!!, description))
    }
}

class AddExpenseUseCase @Inject constructor(private val repository: TransactionRepository) {
    suspend operator fun invoke(user: UserProfile, category: String?, amount: Double?, date: Long?, description: String): Result<SaveResult> {
        if (category.isNullOrBlank()) return Result.failure(IllegalArgumentException("Please choose an expense category."))
        val cartId = runCatching { validate(user, amount, date, description) }.getOrElse { return Result.failure(it) }
        val safeCategory = if (category in ExpenseCategories.all) category else "Miscellaneous"
        return repository.addExpense(NewExpense(cartId, safeCategory, amount!!, date!!, description))
    }
}
