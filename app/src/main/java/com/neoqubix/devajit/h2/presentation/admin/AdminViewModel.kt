package com.neoqubix.devajit.h2.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neoqubix.devajit.h2.domain.model.Cart
import com.neoqubix.devajit.h2.domain.model.CartPerformance
import com.neoqubix.devajit.h2.domain.model.DateFilter
import com.neoqubix.devajit.h2.domain.model.DateSelection
import com.neoqubix.devajit.h2.domain.model.FinancialSummary
import com.neoqubix.devajit.h2.domain.model.ReportRow
import com.neoqubix.devajit.h2.domain.model.Transaction
import com.neoqubix.devajit.h2.domain.model.UserProfile
import com.neoqubix.devajit.h2.domain.usecase.AssignManagerUseCase
import com.neoqubix.devajit.h2.domain.usecase.ObserveCartsUseCase
import com.neoqubix.devajit.h2.domain.usecase.ObserveManagersUseCase
import com.neoqubix.devajit.h2.domain.usecase.ObserveTransactionsUseCase
import com.neoqubix.devajit.h2.domain.usecase.ReportCalculator
import com.neoqubix.devajit.h2.presentation.common.UiState
import com.neoqubix.devajit.h2.presentation.common.asUiState
import com.neoqubix.devajit.h2.utils.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminOverview(
    val carts: List<Cart>,
    val managers: List<UserProfile>,
    val transactions: List<Transaction>,
    val summary: FinancialSummary,
    val performance: List<CartPerformance>,
    val trend: List<ReportRow>,
    val trendIsMonthly: Boolean
) {
    val activeCarts: Int get() = carts.count { it.isActive }
    fun managerName(id: String?): String? = id?.let { m -> managers.find { it.id == m }?.name?.ifBlank { "Unnamed" } }
    fun cartName(id: String?): String? = id?.let { c -> carts.find { it.id == c }?.name }
}

// Shared by the admin Dashboard, Carts and Managers tabs: every cart, every manager, and all records in the chosen dates
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AdminViewModel @Inject constructor(
    observeCarts: ObserveCartsUseCase,
    observeManagers: ObserveManagersUseCase,
    private val observeTransactions: ObserveTransactionsUseCase,
    private val assignManager: AssignManagerUseCase
) : ViewModel() {

    private val user = MutableStateFlow<UserProfile?>(null)
    val selection = MutableStateFlow(DateSelection(DateFilter.THIS_MONTH))

    fun setUser(profile: UserProfile) {
        user.value = profile
    }

    private val carts = observeCarts().asUiState()
    private val managers = observeManagers().asUiState()
    private val transactions = combine(user.filterNotNull().distinctUntilChanged { a, b -> a.id == b.id && a.role == b.role }, selection) { u, s -> u to s }
        .flatMapLatest { (u, s) -> observeTransactions(u, null, s.range).asUiState() }

    val overview: StateFlow<UiState<AdminOverview>> = combine(carts, managers, transactions, selection) { c, m, t, s ->
        val error = listOf(c, m, t).filterIsInstance<UiState.Error>().firstOrNull()
        when {
            error != null -> error
            c is UiState.Success && m is UiState.Success && t is UiState.Success -> {
                val range = s.range
                UiState.Success(
                    AdminOverview(
                        carts = c.data,
                        managers = m.data,
                        transactions = t.data,
                        summary = FinancialSummary.of(t.data),
                        performance = ReportCalculator.cartPerformance(c.data, t.data),
                        trend = ReportCalculator.trend(t.data, range.startDate(), range.endDate()),
                        trendIsMonthly = range.dayCount > 31
                    )
                )
            }
            else -> UiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val assigning = MutableStateFlow(false)

    fun assign(manager: UserProfile, cartId: String?, onDone: () -> Unit) {
        if (assigning.value) return
        viewModelScope.launch {
            assigning.value = true
            assignManager(manager, cartId)
                .onSuccess {
                    _messages.tryEmit(if (cartId == null) "${manager.name} is no longer assigned to a cart." else "${manager.name} assigned.")
                    onDone()
                }
                .onFailure { _messages.tryEmit("Couldn't change the assignment. ${it.toUserMessage()}") }
            assigning.value = false
        }
    }
}
