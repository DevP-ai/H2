package com.neoqubix.devajit.h2.domain.usecase

import com.neoqubix.devajit.h2.domain.model.DateRange
import com.neoqubix.devajit.h2.domain.model.ExpenseCategories
import com.neoqubix.devajit.h2.domain.model.NewExpense
import com.neoqubix.devajit.h2.domain.model.NewRevenue
import com.neoqubix.devajit.h2.domain.model.Role
import com.neoqubix.devajit.h2.domain.model.Transaction
import com.neoqubix.devajit.h2.domain.model.TransactionEdit
import com.neoqubix.devajit.h2.domain.model.TransactionType
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

/*
 * Checks shared by both forms. A manager's cart always comes from their own profile (any cartId passed in is
 * ignored); an admin adds to the cart they opened.
 */
private fun validate(user: UserProfile, cartId: String?, amount: Double?, date: Long?, description: String): String {
    val target = if (user.isAdmin) cartId ?: throw IllegalStateException("Please choose a cart.")
    else user.cartId ?: throw IllegalStateException("Your account has not been assigned to a food cart yet.")
    require(amount != null && amount > 0) { "Please enter an amount greater than 0." }
    require(date != null) { "Please select a date." }
    require(date <= System.currentTimeMillis() + 24L * 60 * 60 * 1000) { "The date can't be in the future." }
    require(description.trim().length <= MAX_DESCRIPTION) { "Description is too long (max $MAX_DESCRIPTION characters)." }
    return target
}

class AddRevenueUseCase @Inject constructor(private val repository: TransactionRepository) {
    suspend operator fun invoke(user: UserProfile, cartId: String?, amount: Double?, date: Long?, description: String): Result<SaveResult> {
        val target = runCatching { validate(user, cartId, amount, date, description) }.getOrElse { return Result.failure(it) }
        return repository.addRevenue(NewRevenue(target, amount!!, date!!, description))
    }
}

// Only admins can correct records; they can do so for every cart
class UpdateTransactionUseCase @Inject constructor(private val repository: TransactionRepository) {
    suspend operator fun invoke(
        user: UserProfile,
        original: Transaction,
        category: String?,
        amount: Double?,
        date: Long?,
        description: String
    ): Result<SaveResult> {
        if (!user.isAdmin) return Result.failure(IllegalStateException("Only an admin can edit revenue and expenses."))
        if (!original.isRevenue && category.isNullOrBlank()) return Result.failure(IllegalArgumentException("Please choose an expense category."))
        runCatching {
            require(amount != null && amount > 0) { "Please enter an amount greater than 0." }
            require(date != null) { "Please select a date." }
            require(date <= System.currentTimeMillis() + 24L * 60 * 60 * 1000) { "The date can't be in the future." }
            require(description.trim().length <= MAX_DESCRIPTION) { "Description is too long (max $MAX_DESCRIPTION characters)." }
        }.getOrElse { return Result.failure(it) }
        val safeCategory = if (original.isRevenue) null else if (category in ExpenseCategories.all) category else "Miscellaneous"
        return repository.updateTransaction(original.type, original.id,
            TransactionEdit(amount!!, date!!, description, safeCategory)
        )
    }
}

class ObserveTransactionUseCase @Inject constructor(private val repository: TransactionRepository) {
    operator fun invoke(type: TransactionType, id: String): Flow<Transaction?> = repository.observeTransaction(type, id)
}

class AddExpenseUseCase @Inject constructor(private val repository: TransactionRepository) {
    suspend operator fun invoke(user: UserProfile, cartId: String?, category: String?, amount: Double?, date: Long?, description: String): Result<SaveResult> {
        if (category.isNullOrBlank()) return Result.failure(IllegalArgumentException("Please choose an expense category."))
        val target = runCatching { validate(user, cartId, amount, date, description) }.getOrElse { return Result.failure(it) }
        val safeCategory = if (category in ExpenseCategories.all) category else "Miscellaneous"
        return repository.addExpense(NewExpense(target, safeCategory, amount!!, date!!, description))
    }
}
