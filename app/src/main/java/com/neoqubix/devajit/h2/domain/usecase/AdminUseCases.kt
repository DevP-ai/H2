package com.neoqubix.devajit.h2.domain.usecase

import com.neoqubix.devajit.h2.domain.model.Cart
import com.neoqubix.devajit.h2.domain.model.CartStatus
import com.neoqubix.devajit.h2.domain.model.UserProfile
import com.neoqubix.devajit.h2.domain.repository.CartRepository
import com.neoqubix.devajit.h2.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveCartUseCase @Inject constructor(private val repository: CartRepository) {
    operator fun invoke(cartId: String): Flow<Cart?> = repository.observeCart(cartId)
}

class ObserveCartsUseCase @Inject constructor(private val repository: CartRepository) {
    operator fun invoke(): Flow<List<Cart>> = repository.observeCarts()
}

class ObserveManagersUseCase @Inject constructor(private val repository: UserRepository) {
    operator fun invoke(): Flow<List<UserProfile>> = repository.observeManagers()
}

// Creates or edits a cart, then gives it the chosen manager (null = no manager)
class SaveCartUseCase @Inject constructor(private val repository: CartRepository) {
    suspend operator fun invoke(
        existing: Cart?,
        name: String,
        location: String,
        status: CartStatus,
        managerId: String?
    ): Result<String> {
        if (name.isBlank()) return Result.failure(IllegalArgumentException("Please enter the cart name."))
        if (name.trim().length > 80) return Result.failure(IllegalArgumentException("Cart name is too long."))
        if (location.trim().length > 120) return Result.failure(IllegalArgumentException("Location is too long."))

        val cartId = if (existing == null) {
            repository.createCart(name, location, status).getOrElse { return Result.failure(it) }
        } else {
            repository.updateCart(existing.id, name, location, status).getOrElse { return Result.failure(it) }
            existing.id
        }
        if (existing?.managerId != managerId) {
            repository.assignManager(cartId, managerId).getOrElse { return Result.failure(it) }
        }
        return Result.success(cartId)
    }
}

class AssignManagerUseCase @Inject constructor(private val repository: CartRepository) {
    // cartId null takes the manager off their cart
    suspend operator fun invoke(manager: UserProfile, cartId: String?): Result<Unit> = when {
        manager.isAdmin -> Result.failure(IllegalArgumentException("Admins can't be assigned to a cart."))
        cartId == manager.cartId -> Result.success(Unit)
        cartId == null -> repository.unassignManager(manager.id)
        else -> repository.assignManager(cartId, manager.id)
    }
}
