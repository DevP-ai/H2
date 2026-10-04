package com.neoqubix.devajit.h2.presentation.admin.carts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neoqubix.devajit.h2.domain.model.Cart
import com.neoqubix.devajit.h2.domain.model.DateFilter
import com.neoqubix.devajit.h2.domain.model.DateSelection
import com.neoqubix.devajit.h2.domain.model.FinancialSummary
import com.neoqubix.devajit.h2.domain.model.Transaction
import com.neoqubix.devajit.h2.domain.model.UserProfile
import com.neoqubix.devajit.h2.domain.usecase.ObserveCartUseCase
import com.neoqubix.devajit.h2.domain.usecase.ObserveManagersUseCase
import com.neoqubix.devajit.h2.domain.usecase.ObserveTransactionsUseCase
import com.neoqubix.devajit.h2.presentation.common.UiState
import com.neoqubix.devajit.h2.presentation.common.asUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// One cart for an admin: details, totals for the chosen dates, and its sales and expenses (paged)
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CartDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeCart: ObserveCartUseCase,
    observeManagers: ObserveManagersUseCase,
    private val observeTransactions: ObserveTransactionsUseCase
) : ViewModel() {

    val cartId: String = checkNotNull(savedStateHandle["cartId"])
    private val user = MutableStateFlow<UserProfile?>(null)

    fun setUser(profile: UserProfile) {
        user.value = profile
    }

    val cart: StateFlow<UiState<Cart?>> = observeCart(cartId).asUiState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    val managerName: StateFlow<String?> = combine(cart, observeManagers().catch { emit(emptyList()) }) { c, managers ->
        val id = (c as? UiState.Success)?.data?.managerId
        id?.let { m -> managers.find { it.id == m }?.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val selection = MutableStateFlow(DateSelection(DateFilter.THIS_MONTH))
    private val limit = MutableStateFlow(PAGE_SIZE)

    val summary: StateFlow<UiState<FinancialSummary>> = combine(user.filterNotNull(), selection) { u, s -> u to s }
        .flatMapLatest { (u, s) -> observeTransactions(u, cartId, s.range).map { FinancialSummary.of(it) }.asUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    val transactions: StateFlow<UiState<List<Transaction>>> = combine(user.filterNotNull(), selection, limit) { u, s, l -> Triple(u, s, l) }
        .flatMapLatest { (u, s, l) -> observeTransactions(u, cartId, s.range, l).asUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    val canLoadMore: StateFlow<Boolean> = combine(transactions, limit) { t, l -> (t as? UiState.Success)?.data?.size?.toLong() == l }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setSelection(value: DateSelection) {
        selection.value = value
        limit.value = PAGE_SIZE
    }

    fun loadMore() {
        limit.value += PAGE_SIZE
    }

    private companion object {
        const val PAGE_SIZE = 50L
    }
}
