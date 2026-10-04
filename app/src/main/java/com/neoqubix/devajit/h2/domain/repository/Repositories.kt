package com.neoqubix.devajit.h2.domain.repository

import com.neoqubix.devajit.h2.domain.model.Cart
import com.neoqubix.devajit.h2.domain.model.CartStatus
import com.neoqubix.devajit.h2.domain.model.DateRange
import com.neoqubix.devajit.h2.domain.model.NewExpense
import com.neoqubix.devajit.h2.domain.model.NewRevenue
import com.neoqubix.devajit.h2.domain.model.Transaction
import com.neoqubix.devajit.h2.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    // Signed-in user's uid, or null
    fun observeAuthUid(): Flow<String?>
    // True between creating the account and writing its profile, so the app doesn't report a missing profile
    val registrationInProgress: StateFlow<Boolean>
    suspend fun login(email: String, password: String): Result<Unit>
    // Creates the Firebase account and its users/{uid} document (role "manager", no cart)
    suspend fun register(name: String, email: String, password: String): Result<Unit>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    fun logout()
}

interface UserRepository {
    // null when the users/{uid} document doesn't exist
    fun observeUser(uid: String): Flow<UserProfile?>
    fun observeManagers(): Flow<List<UserProfile>>
    suspend fun updateOwnProfile(uid: String, name: String, phone: String): Result<Unit>
}

interface CartRepository {
    fun observeCart(cartId: String): Flow<Cart?>
    fun observeCarts(): Flow<List<Cart>>
    suspend fun createCart(name: String, location: String, status: CartStatus): Result<String>
    suspend fun updateCart(cartId: String, name: String, location: String, status: CartStatus): Result<Unit>
    // Makes managerId the cart's only manager (null removes the manager), keeping users/{uid}.cartId in step
    suspend fun assignManager(cartId: String, managerId: String?): Result<Unit>
    // Takes the manager off whatever cart they have
    suspend fun unassignManager(managerId: String): Result<Unit>
}

enum class SaveResult { SAVED, QUEUED_OFFLINE }

interface TransactionRepository {
    // Sales and expenses in the range, newest first. cartId null means every cart (admins only).
    // limit caps each of sales and expenses for paging; null loads the whole range.
    fun observeTransactions(cartId: String?, range: DateRange, limit: Long? = null): Flow<List<Transaction>>
    suspend fun addRevenue(revenue: NewRevenue): Result<SaveResult>
    suspend fun addExpense(expense: NewExpense): Result<SaveResult>
    // Writes saved offline that the server later rejected
    val syncErrors: SharedFlow<String>
}
