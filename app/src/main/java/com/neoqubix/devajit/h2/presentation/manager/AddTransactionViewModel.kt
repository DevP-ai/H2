package com.neoqubix.devajit.h2.presentation.manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neoqubix.devajit.h2.domain.model.UserProfile
import com.neoqubix.devajit.h2.domain.repository.SaveResult
import com.neoqubix.devajit.h2.domain.usecase.AddExpenseUseCase
import com.neoqubix.devajit.h2.domain.usecase.AddRevenueUseCase
import com.neoqubix.devajit.h2.utils.parseAmount
import com.neoqubix.devajit.h2.utils.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

data class TransactionForm(
    val amount: String = "",
    val date: LocalDate = LocalDate.now(),
    val description: String = "",
    val category: String? = null,
    val amountError: String? = null,
    val categoryError: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    // Set once saved; the screen shows the message and closes
    val savedMessage: String? = null
)

// The Add Revenue and Add Expense forms
@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val addRevenue: AddRevenueUseCase,
    private val addExpense: AddExpenseUseCase
) : ViewModel() {

    private val _form = MutableStateFlow(TransactionForm())
    val form: StateFlow<TransactionForm> = _form.asStateFlow()

    fun onAmount(value: String) {
        // Digits and one decimal point, up to 2 decimals
        if (value.isEmpty() || value.matches(Regex("^\\d{0,8}(\\.\\d{0,2})?$"))) {
            _form.update { it.copy(amount = value, amountError = null, error = null) }
        }
    }

    fun onDate(value: LocalDate) = _form.update { it.copy(date = value, error = null) }
    fun onDescription(value: String) = _form.update { it.copy(description = value.take(200), error = null) }
    fun onCategory(value: String) = _form.update { it.copy(category = value, categoryError = false, error = null) }

    // The record's time: now for today, noon for earlier days, so it falls on the chosen date
    private fun dateMillis(date: LocalDate): Long {
        val time = if (date == LocalDate.now()) LocalTime.now() else LocalTime.NOON
        return LocalDateTime.of(date, time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun save(user: UserProfile, isExpense: Boolean) {
        val f = _form.value
        if (f.saving) return
        val amount = parseAmount(f.amount)
        val categoryMissing = isExpense && f.category == null
        if (amount == null || categoryMissing) {
            _form.update {
                it.copy(
                    amountError = if (amount == null) "Enter an amount greater than 0" else null,
                    categoryError = categoryMissing
                )
            }
            return
        }
        viewModelScope.launch {
            _form.update { it.copy(saving = true, error = null) }
            val result = if (isExpense) {
                addExpense(user, f.category, amount, dateMillis(f.date), f.description)
            } else {
                addRevenue(user, amount, dateMillis(f.date), f.description)
            }
            val what = if (isExpense) "Expense" else "Revenue"
            result
                .onSuccess { saved ->
                    val message = if (saved == SaveResult.QUEUED_OFFLINE) "$what saved on this phone. It will sync when you're back online." else "$what saved."
                    _form.update { it.copy(saving = false, savedMessage = message) }
                }
                .onFailure { e ->
                    _form.update { it.copy(saving = false, error = "Unable to save the ${what.lowercase()}. ${e.toUserMessage()}") }
                }
        }
    }
}
