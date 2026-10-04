package com.neoqubix.devajit.h2.presentation.admin.carts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neoqubix.devajit.h2.domain.model.Cart
import com.neoqubix.devajit.h2.domain.model.CartStatus
import com.neoqubix.devajit.h2.domain.model.UserProfile
import com.neoqubix.devajit.h2.domain.usecase.ObserveCartUseCase
import com.neoqubix.devajit.h2.domain.usecase.ObserveManagersUseCase
import com.neoqubix.devajit.h2.domain.usecase.SaveCartUseCase
import com.neoqubix.devajit.h2.utils.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CartForm(
    val loading: Boolean = true,
    val existing: Cart? = null,
    val name: String = "",
    val location: String = "",
    val status: CartStatus = CartStatus.ACTIVE,
    val managerId: String? = null,
    val nameError: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

// Add Cart (cartId "new") and Edit Cart
@HiltViewModel
class CartEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeCart: ObserveCartUseCase,
    observeManagers: ObserveManagersUseCase,
    private val saveCart: SaveCartUseCase
) : ViewModel() {

    private val cartId: String? = savedStateHandle.get<String>("cartId")?.takeIf { it != NEW }

    private val _form = MutableStateFlow(CartForm(loading = cartId != null))
    val form: StateFlow<CartForm> = _form.asStateFlow()

    val managers: StateFlow<List<UserProfile>> = observeManagers().catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (cartId != null) {
            viewModelScope.launch {
                runCatching { observeCart(cartId).first() }
                    .onSuccess { cart ->
                        _form.value = if (cart == null) CartForm(loading = false, error = "Cart not found.")
                        else CartForm(false, cart, cart.name, cart.location, cart.status, cart.managerId)
                    }
                    .onFailure { e -> _form.value = CartForm(loading = false, error = e.toUserMessage()) }
            }
        }
    }

    fun onName(v: String) = _form.update { it.copy(name = v.take(80), nameError = false, error = null) }
    fun onLocation(v: String) = _form.update { it.copy(location = v.take(120), error = null) }
    fun onStatus(v: CartStatus) = _form.update { it.copy(status = v, error = null) }
    fun onManager(v: String?) = _form.update { it.copy(managerId = v, error = null) }

    fun save() {
        val f = _form.value
        if (f.saving) return
        if (f.name.isBlank()) {
            _form.update { it.copy(nameError = true) }
            return
        }
        viewModelScope.launch {
            _form.update { it.copy(saving = true, error = null) }
            saveCart(f.existing, f.name, f.location, f.status, f.managerId)
                .onSuccess { _form.update { it.copy(saving = false, saved = true) } }
                .onFailure { e -> _form.update { it.copy(saving = false, error = e.toUserMessage()) } }
        }
    }

    companion object {
        const val NEW = "new"
    }
}
