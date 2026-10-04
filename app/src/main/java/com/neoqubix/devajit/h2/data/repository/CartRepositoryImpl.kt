package com.neoqubix.devajit.h2.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.neoqubix.devajit.h2.data.firebase.Collections
import com.neoqubix.devajit.h2.data.firebase.Fields
import com.neoqubix.devajit.h2.data.firebase.snapshots
import com.neoqubix.devajit.h2.data.firebase.toCart
import com.neoqubix.devajit.h2.domain.model.Cart
import com.neoqubix.devajit.h2.domain.model.CartStatus
import com.neoqubix.devajit.h2.domain.model.Role
import com.neoqubix.devajit.h2.domain.repository.CartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CartRepository {

    private val carts get() = firestore.collection(Collections.CARTS)
    private val users get() = firestore.collection(Collections.USERS)

    override fun observeCart(cartId: String): Flow<Cart?> =
        carts.document(cartId).snapshots().map { if (it.exists()) it.toCart() else null }

    override fun observeCarts(): Flow<List<Cart>> =
        carts.snapshots().map { snapshot -> snapshot.documents.map { it.toCart() }.sortedBy { it.name.lowercase() } }

    override suspend fun createCart(name: String, location: String, status: CartStatus): Result<String> = runCatching {
        val ref = carts.document()
        ref.set(
            mapOf(
                Fields.NAME to name.trim(),
                Fields.LOCATION to location.trim(),
                Fields.MANAGER_ID to null,
                Fields.STATUS to status.value,
                Fields.CREATED_AT to FieldValue.serverTimestamp(),
                Fields.UPDATED_AT to FieldValue.serverTimestamp()
            )
        ).awaitOrQueued()
        ref.id
    }

    override suspend fun updateCart(cartId: String, name: String, location: String, status: CartStatus): Result<Unit> = runCatching {
        carts.document(cartId).update(
            mapOf(
                Fields.NAME to name.trim(),
                Fields.LOCATION to location.trim(),
                Fields.STATUS to status.value,
                Fields.UPDATED_AT to FieldValue.serverTimestamp()
            )
        ).awaitOrQueued()
        Unit
    }

    // One manager per cart and one cart per manager, kept consistent on both documents in a single transaction
    override suspend fun assignManager(cartId: String, managerId: String?): Result<Unit> = runCatching {
        firestore.runTransaction { tx ->
            // All reads first
            val cartRef = carts.document(cartId)
            val cart = tx.get(cartRef)
            if (!cart.exists()) throw IllegalStateException("Cart not found.")
            val previousManagerId = cart.getString(Fields.MANAGER_ID)?.ifBlank { null }
            val previousManager = previousManagerId?.takeIf { it != managerId }?.let { tx.get(users.document(it)) }

            val manager = managerId?.let { tx.get(users.document(it)) }
            if (manager != null) {
                if (!manager.exists()) throw IllegalStateException("Manager not found.")
                if (manager.getString(Fields.ROLE) != Role.MANAGER.value) throw IllegalStateException("Only managers can be assigned to a cart.")
            }
            val managersOldCartId = manager?.getString(Fields.CART_ID)?.ifBlank { null }?.takeIf { it != cartId }
            val managersOldCart: DocumentSnapshot? = managersOldCartId?.let { tx.get(carts.document(it)) }

            // Then all writes
            val now = FieldValue.serverTimestamp()
            if (previousManager != null && previousManager.exists() && previousManager.getString(Fields.CART_ID) == cartId) {
                tx.update(previousManager.reference, mapOf(Fields.CART_ID to null, Fields.UPDATED_AT to now))
            }
            if (managersOldCart != null && managersOldCart.exists() && managersOldCart.getString(Fields.MANAGER_ID) == managerId) {
                tx.update(managersOldCart.reference, mapOf(Fields.MANAGER_ID to null, Fields.UPDATED_AT to now))
            }
            tx.update(cartRef, mapOf(Fields.MANAGER_ID to managerId, Fields.UPDATED_AT to now))
            if (manager != null) {
                tx.update(manager.reference, mapOf(Fields.CART_ID to cartId, Fields.UPDATED_AT to now))
            }
            null
        }.await()
        Unit
    }

    override suspend fun unassignManager(managerId: String): Result<Unit> = runCatching {
        firestore.runTransaction { tx ->
            val managerRef = users.document(managerId)
            val manager = tx.get(managerRef)
            if (!manager.exists()) throw IllegalStateException("Manager not found.")
            val cartId = manager.getString(Fields.CART_ID)?.ifBlank { null } ?: return@runTransaction null
            val cart = tx.get(carts.document(cartId))

            val now = FieldValue.serverTimestamp()
            tx.update(managerRef, mapOf(Fields.CART_ID to null, Fields.UPDATED_AT to now))
            if (cart.exists() && cart.getString(Fields.MANAGER_ID) == managerId) {
                tx.update(cart.reference, mapOf(Fields.MANAGER_ID to null, Fields.UPDATED_AT to now))
            }
            null
        }.await()
        Unit
    }
}
