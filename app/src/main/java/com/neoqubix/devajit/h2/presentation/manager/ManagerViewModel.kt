package com.neoqubix.devajit.h2.presentation.manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neoqubix.devajit.h2.domain.model.Cart
import com.neoqubix.devajit.h2.domain.model.DateFilter
import com.neoqubix.devajit.h2.domain.model.DateSelection
import com.neoqubix.devajit.h2.domain.model.Transaction
import com.neoqubix.devajit.h2.domain.model.UserProfile
import com.neoqubix.devajit.h2.domain.usecase.ObserveCartUseCase
import com.neoqubix.devajit.h2.domain.usecase.ObserveTransactionsUseCase
import com.neoqubix.devajit.h2.presentation.common.UiState
import com.neoqubix.devajit.h2.presentation.common.asUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/*
 * Shared by the manager's Home and Transactions tabs. Everything is read for the manager's own cart only:
 * the cart id comes from their profile, never from the screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ManagerViewModel @Inject constructor(
    observeCart: ObserveCartUseCase,
    private val observeTransactions: ObserveTransactionsUseCase
) : ViewModel() {

    private val user = MutableStateFlow<UserProfile?>(null)

    // Only the parts that change what is queried
    private val scope = user.filterNotNull().distinctUntilChanged { a, b -> a.id == b.id && a.cartId == b.cartId && a.role == b.role }

    fun setUser(profile: UserProfile) {
        user.value = profile
    }

    // Success(null) when the cart document is missing
    val cart: StateFlow<UiState<Cart?>> = scope
        .flatMapLatest { u -> u.cartId?.let { observeCart(it).asUiState() } ?: flowOf(UiState.Success(null)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    // ----- Home -----

    val homeSelection = MutableStateFlow(DateSelection(DateFilter.TODAY))

    val homeTransactions: StateFlow<UiState<List<Transaction>>> = combine(scope, homeSelection) { u, s -> u to s }
        .flatMapLatest { (u, s) -> observeTransactions(u, u.cartId, s.range).asUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    // ----- Transaction history (paged) -----

    val historySelection = MutableStateFlow(DateSelection(DateFilter.THIS_MONTH))
    private val historyLimit = MutableStateFlow(PAGE_SIZE)

    val history: StateFlow<UiState<List<Transaction>>> = combine(scope, historySelection, historyLimit) { u, s, l -> Triple(u, s, l) }
        .flatMapLatest { (u, s, l) -> observeTransactions(u, u.cartId, s.range, l).asUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    // More pages may exist while a full page came back
    val canLoadMore: StateFlow<Boolean> = combine(history, historyLimit) { h, l ->
        (h as? UiState.Success)?.data?.size?.toLong() == l
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setHistorySelection(selection: DateSelection) {
        historySelection.value = selection
        historyLimit.value = PAGE_SIZE
    }

    fun loadMore() {
        historyLimit.value += PAGE_SIZE
    }

    private companion object {
        const val PAGE_SIZE = 50L
    }
}
