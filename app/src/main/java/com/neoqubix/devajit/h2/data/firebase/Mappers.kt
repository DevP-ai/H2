package com.neoqubix.devajit.h2.data.firebase

import com.google.firebase.firestore.DocumentSnapshot
import com.neoqubix.devajit.h2.domain.model.Cart
import com.neoqubix.devajit.h2.domain.model.CartStatus
import com.neoqubix.devajit.h2.domain.model.Role
import com.neoqubix.devajit.h2.domain.model.Transaction
import com.neoqubix.devajit.h2.domain.model.TransactionType
import com.neoqubix.devajit.h2.domain.model.UserProfile

private fun DocumentSnapshot.millis(field: String): Long? = getTimestamp(field)?.toDate()?.time

fun DocumentSnapshot.toUserProfile(): UserProfile = UserProfile(
    id = id,
    name = getString(Fields.NAME).orEmpty(),
    email = getString(Fields.EMAIL).orEmpty(),
    phone = getString(Fields.PHONE).orEmpty(),
    role = Role.from(getString(Fields.ROLE)),
    cartId = getString(Fields.CART_ID)?.ifBlank { null },
    createdAt = millis(Fields.CREATED_AT)
)

fun DocumentSnapshot.toCart(): Cart = Cart(
    id = id,
    name = getString(Fields.NAME).orEmpty(),
    location = getString(Fields.LOCATION).orEmpty(),
    managerId = getString(Fields.MANAGER_ID)?.ifBlank { null },
    status = CartStatus.from(getString(Fields.STATUS)),
    createdAt = millis(Fields.CREATED_AT)
)

// null for a malformed record (missing amount or date), which is skipped
fun DocumentSnapshot.toTransaction(type: TransactionType): Transaction? {
    val amount = getDouble(Fields.AMOUNT) ?: return null
    val date = millis(Fields.DATE) ?: return null
    return Transaction(
        id = id,
        type = type,
        cartId = getString(Fields.CART_ID).orEmpty(),
        amount = amount,
        date = date,
        description = getString(Fields.DESCRIPTION).orEmpty(),
        category = if (type == TransactionType.EXPENSE) getString(Fields.CATEGORY) ?: "Miscellaneous" else null,
        createdBy = getString(Fields.CREATED_BY).orEmpty(),
        createdAt = millis(Fields.CREATED_AT),
        updatedBy = getString(Fields.UPDATED_BY)?.ifBlank { null },
        updatedAt = millis(Fields.UPDATED_AT),
        pendingSync = metadata.hasPendingWrites()
    )
}
