package com.neoqubix.devajit.h2.presentation.admin.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neoqubix.devajit.h2.domain.model.Cart
import com.neoqubix.devajit.h2.domain.model.Transaction
import com.neoqubix.devajit.h2.domain.model.TransactionType
import com.neoqubix.devajit.h2.domain.model.UserProfile
import com.neoqubix.devajit.h2.domain.repository.SaveResult
import com.neoqubix.devajit.h2.domain.usecase.ObserveCartUseCase
import com.neoqubix.devajit.h2.domain.usecase.ObserveManagersUseCase
import com.neoqubix.devajit.h2.domain.usecase.ObserveTransactionUseCase
import com.neoqubix.devajit.h2.domain.usecase.UpdateTransactionUseCase
import com.neoqubix.devajit.h2.utils.parseAmount
import com.neoqubix.devajit.h2.utils.toLocalDate
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
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class EditForm(
    val loading: Boolean = true,
    val original: Transaction? = null,
    val cart: Cart? = null,
    val amount: String = "",
    val date: LocalDate = LocalDate.now(),
    val description: String = "",
    val category: String? = null,
    val amountError: String? = null,
    val saving: Boolean = false,
    val error: String? = null,
    val savedMessage: String? = null
)

// Admin correction of one revenue or expense record, from any cart
@HiltViewModel
class EditTransactionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeTransaction: ObserveTransactionUseCase,
    observeCart: ObserveCartUseCase,
    observeManagers: ObserveManagersUseCase,
    private val updateTransaction: UpdateTransactionUseCase
) : ViewModel() {

    private val type = TransactionType.valueOf(checkNotNull(savedStateHandle.get<String>("type")))
    private val id: String = checkNotNull(savedStateHandle["id"])

    private val _form = MutableStateFlow(EditForm())
    val form: StateFlow<EditForm> = _form.asStateFlow()

    // To show who created / last edited the record
    val people: StateFlow<List<UserProfile>> = observeManagers().catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            runCatching { observeTransaction(type, id).first() }
                .onSuccess { t ->
                    if (t == null) {
                        _form.value = EditForm(loading = false, error = "This record no longer exists.")
                        return@onSuccess
                    }
                    val cart = runCatching { observeCart(t.cartId).first() }.getOrNull()
                    _form.value = EditForm(
                        loading = false,
                        original = t,
                        cart = cart,
                        amount = t.amount.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() },
                        date = t.date.toLocalDate(),
                        description = t.description,
                        category = t.category
                    )
                }
                .onFailure { e -> _form.value = EditForm(loading = false, error = e.toUserMessage()) }
        }
    }

    fun onAmount(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d{0,8}(\\.\\d{0,2})?$"))) {
            _form.update { it.copy(amount = value, amountError = null, error = null) }
        }
    }

    fun onDate(value: LocalDate) = _form.update { it.copy(date = value, error = null) }
    fun onDescription(value: String) = _form.update { it.copy(description = value.take(200), error = null) }
    fun onCategory(value: String) = _form.update { it.copy(category = value, error = null) }

    val hasChanges: Boolean
        get() {
            val f = _form.value
            val o = f.original ?: return false
            return parseAmount(f.amount) != o.amount || f.date != o.date.toLocalDate() ||
                f.description.trim() != o.description || (!o.isRevenue && f.category != o.category)
        }

    fun save(user: UserProfile) {
        val f = _form.value
        val original = f.original ?: return
        if (f.saving) return
        val amount = parseAmount(f.amount)
        if (amount == null) {
            _form.update { it.copy(amountError = "Enter an amount greater than 0") }
            return
        }
        // Keep the original time of day when the date is unchanged; move to noon on a new date
        val zone = ZoneId.systemDefault()
        val date = if (f.date == original.date.toLocalDate()) original.date
        else f.date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

        viewModelScope.launch {
            _form.update { it.copy(saving = true, error = null) }
            updateTransaction(user, original, f.category, amount, date, f.description)
                .onSuccess { saved ->
                    val message = if (saved == SaveResult.QUEUED_OFFLINE) "Change saved on this phone. It will sync when you're back online." else "Changes saved."
                    _form.update { it.copy(saving = false, savedMessage = message) }
                }
                .onFailure { e -> _form.update { it.copy(saving = false, error = "Unable to save the change. ${e.toUserMessage()}") } }
        }
    }
}
