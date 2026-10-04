package com.neoqubix.devajit.h2.presentation.admin.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neoqubix.devajit.h2.domain.model.Cart
import com.neoqubix.devajit.h2.domain.model.DateFilter
import com.neoqubix.devajit.h2.domain.model.DateSelection
import com.neoqubix.devajit.h2.domain.model.FinancialSummary
import com.neoqubix.devajit.h2.domain.model.ReportRow
import com.neoqubix.devajit.h2.domain.model.ReportType
import com.neoqubix.devajit.h2.domain.model.UserProfile
import com.neoqubix.devajit.h2.domain.usecase.ObserveCartsUseCase
import com.neoqubix.devajit.h2.domain.usecase.ObserveTransactionsUseCase
import com.neoqubix.devajit.h2.domain.usecase.ReportCalculator
import com.neoqubix.devajit.h2.presentation.common.UiState
import com.neoqubix.devajit.h2.presentation.common.asUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ReportData(
    val summary: FinancialSummary,
    val rows: List<ReportRow>,
    // Chart points: per day up to 31 days, otherwise per month
    val trend: List<ReportRow>,
    val trendIsMonthly: Boolean
)

// Reports for admins (all carts or one) and managers (their own cart only)
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportsViewModel @Inject constructor(
    observeCarts: ObserveCartsUseCase,
    private val observeTransactions: ObserveTransactionsUseCase
) : ViewModel() {

    private val user = MutableStateFlow<UserProfile?>(null)
    private val scope = user.filterNotNull().distinctUntilChanged { a, b -> a.id == b.id && a.cartId == b.cartId && a.role == b.role }

    val selection = MutableStateFlow(DateSelection(DateFilter.THIS_MONTH))
    val type = MutableStateFlow(ReportType.DAILY)
    // Admin only: null = all carts
    val cartFilter = MutableStateFlow<String?>(null)

    fun setUser(profile: UserProfile) {
        user.value = profile
    }

    val carts: StateFlow<List<Cart>> = scope
        .flatMapLatest { u -> if (u.isAdmin) observeCarts().catch { emit(emptyList()) } else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val transactions = combine(scope, selection, cartFilter) { u, s, c -> Triple(u, s, c) }
        .flatMapLatest { (u, s, c) -> observeTransactions(u, if (u.isAdmin) c else u.cartId, s.range).asUiState() }

    val report: StateFlow<UiState<ReportData>> = combine(transactions, type, carts, selection) { state, t, cartList, s ->
        when (state) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> state
            is UiState.Success -> {
                val list = state.data
                val range = s.range
                val trend = ReportCalculator.trend(list, range.startDate(), range.endDate())
                UiState.Success(
                    ReportData(
                        summary = FinancialSummary.of(list),
                        rows = ReportCalculator.report(t, list, cartList),
                        trend = trend,
                        trendIsMonthly = range.dayCount > 31
                    )
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
}
