package com.neoqubix.devajit.h2.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.neoqubix.devajit.h2.data.firebase.Collections
import com.neoqubix.devajit.h2.data.firebase.Fields
import com.neoqubix.devajit.h2.data.firebase.snapshots
import com.neoqubix.devajit.h2.data.firebase.toTransaction
import com.neoqubix.devajit.h2.domain.model.DateRange
import com.neoqubix.devajit.h2.domain.model.NewExpense
import com.neoqubix.devajit.h2.domain.model.NewRevenue
import com.neoqubix.devajit.h2.domain.model.Transaction
import com.neoqubix.devajit.h2.domain.model.TransactionType
import com.neoqubix.devajit.h2.domain.repository.SaveResult
import com.neoqubix.devajit.h2.domain.repository.TransactionRepository
import com.neoqubix.devajit.h2.utils.toUserMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : TransactionRepository {

    private val syncErrorEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    override val syncErrors: SharedFlow<String> = syncErrorEvents.asSharedFlow()

    // Only the needed cart and dates are read: managers always pass their own cartId (the rules require it)
    private fun rangeQuery(collection: String, cartId: String?, range: DateRange, limit: Long?): Query {
        var query: Query = firestore.collection(collection)
        if (cartId != null) query = query.whereEqualTo(Fields.CART_ID, cartId)
        query = query
            .whereGreaterThanOrEqualTo(Fields.DATE, Timestamp(Date(range.start)))
            .whereLessThan(Fields.DATE, Timestamp(Date(range.endExclusive)))
            .orderBy(Fields.DATE, Query.Direction.DESCENDING)
        if (limit != null) query = query.limit(limit)
        return query
    }

    override fun observeTransactions(cartId: String?, range: DateRange, limit: Long?): Flow<List<Transaction>> {
        val sales = rangeQuery(Collections.SALES, cartId, range, limit).snapshots()
            .map { s -> s.documents.mapNotNull { it.toTransaction(TransactionType.REVENUE) } }
        val expenses = rangeQuery(Collections.EXPENSES, cartId, range, limit).snapshots()
            .map { s -> s.documents.mapNotNull { it.toTransaction(TransactionType.EXPENSE) } }
        return combine(sales, expenses) { s, e ->
            val merged = (s + e).sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.createdAt ?: Long.MAX_VALUE })
            if (limit != null) merged.take(limit.toInt()) else merged
        }
    }

    override suspend fun addRevenue(revenue: NewRevenue): Result<SaveResult> = save(
        Collections.SALES,
        mapOf(
            Fields.CART_ID to revenue.cartId,
            Fields.AMOUNT to revenue.amount,
            Fields.DATE to Timestamp(Date(revenue.date)),
            Fields.DESCRIPTION to revenue.description.trim()
        ),
        what = "revenue"
    )

    override suspend fun addExpense(expense: NewExpense): Result<SaveResult> = save(
        Collections.EXPENSES,
        mapOf(
            Fields.CART_ID to expense.cartId,
            Fields.CATEGORY to expense.category,
            Fields.AMOUNT to expense.amount,
            Fields.DATE to Timestamp(Date(expense.date)),
            Fields.DESCRIPTION to expense.description.trim()
        ),
        what = "expense"
    )

    private suspend fun save(collection: String, fields: Map<String, Any?>, what: String): Result<SaveResult> = runCatching {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Please log in again.")
        // Audit fields are always added here, never taken from the form
        val data = fields + mapOf(
            Fields.CREATED_BY to uid,
            Fields.CREATED_AT to FieldValue.serverTimestamp(),
            Fields.UPDATED_AT to FieldValue.serverTimestamp()
        )
        val task = firestore.collection(collection).document().set(data)
        if (task.awaitOrQueued()) {
            SaveResult.SAVED
        } else {
            // Saved on the phone; tell the user later if the server rejects it when it syncs
            task.addOnFailureListener { e ->
                syncErrorEvents.tryEmit("An offline $what could not be synced: ${e.toUserMessage()}")
            }
            SaveResult.QUEUED_OFFLINE
        }
    }
}
