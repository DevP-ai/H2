package com.neoqubix.devajit.h2.presentation.admin.transactions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.neoqubix.devajit.h2.domain.model.Cart
import com.neoqubix.devajit.h2.domain.model.UserProfile
import com.neoqubix.devajit.h2.domain.usecase.ObserveCartUseCase
import com.neoqubix.devajit.h2.presentation.common.UiState
import com.neoqubix.devajit.h2.presentation.common.asUiState
import com.neoqubix.devajit.h2.presentation.components.EmptyState
import com.neoqubix.devajit.h2.presentation.components.ErrorView
import com.neoqubix.devajit.h2.presentation.components.LoadingView
import com.neoqubix.devajit.h2.presentation.manager.AddTransactionScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CartLoaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeCart: ObserveCartUseCase
) : ViewModel() {
    val cart: StateFlow<UiState<Cart?>> = observeCart(checkNotNull(savedStateHandle.get<String>("cartId"))).asUiState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
}

// The same Add Revenue / Add Expense form managers use, for the cart the admin opened
@Composable
fun AdminAddTransactionRoute(
    user: UserProfile,
    isExpense: Boolean,
    onDone: () -> Unit,
    viewModel: CartLoaderViewModel = hiltViewModel()
) {
    val state by viewModel.cart.collectAsStateWithLifecycle()
    when (val s = state) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingView() }
        is UiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { ErrorView(s.message) }
        is UiState.Success -> {
            val cart = s.data
            if (cart == null) EmptyState("Cart not found.")
            else AddTransactionScreen(user, cart, isExpense, onDone)
        }
    }
}
