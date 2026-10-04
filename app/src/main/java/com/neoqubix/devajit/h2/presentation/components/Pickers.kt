package com.neoqubix.devajit.h2.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neoqubix.devajit.h2.domain.model.DateFilter
import com.neoqubix.devajit.h2.domain.model.DateRange
import com.neoqubix.devajit.h2.domain.model.DateSelection
import com.neoqubix.devajit.h2.domain.model.ExpenseCategories
import com.neoqubix.devajit.h2.utils.formatDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// Material date pickers work in UTC midnight millis; the app works in local dates
private fun LocalDate.toPickerMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun Long.fromPickerMillis(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@OptIn(ExperimentalMaterial3Api::class)
private object PastOrToday : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean = !utcTimeMillis.fromPickerMillis().isAfter(LocalDate.now())
    override fun isSelectableYear(year: Int): Boolean = year <= LocalDate.now().year
}

// Today / Yesterday / This Week / This Month / This Year / Custom, plus the dates being shown
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFilterBar(
    selection: DateSelection,
    onSelectionChange: (DateSelection) -> Unit,
    modifier: Modifier = Modifier,
    filters: List<DateFilter> = DateFilter.entries
) {
    var showCustomPicker by remember { mutableStateOf(false) }
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { filter ->
                FilterChip(
                    selected = selection.filter == filter,
                    onClick = {
                        if (filter == DateFilter.CUSTOM) showCustomPicker = true
                        else onSelectionChange(DateSelection(filter))
                    },
                    label = { Text(filter.label) },
                    leadingIcon = if (filter == DateFilter.CUSTOM) {
                        { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) }
                    } else null
                )
            }
        }
        Text(
            selection.range.label(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showCustomPicker) {
        val current = selection.custom
        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = current?.startDate()?.toPickerMillis(),
            initialSelectedEndDateMillis = current?.endDate()?.toPickerMillis(),
            selectableDates = PastOrToday
        )
        DatePickerDialog(
            onDismissRequest = { showCustomPicker = false },
            confirmButton = {
                TextButton(
                    enabled = state.selectedStartDateMillis != null,
                    onClick = {
                        val start = state.selectedStartDateMillis!!.fromPickerMillis()
                        val end = state.selectedEndDateMillis?.fromPickerMillis() ?: start
                        onSelectionChange(DateSelection(DateFilter.CUSTOM, DateRange.ofDates(start, end)))
                        showCustomPicker = false
                    }
                ) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { showCustomPicker = false }) { Text("Cancel") } }
        ) {
            DateRangePicker(state = state, modifier = Modifier.height(480.dp), showModeToggle = false)
        }
    }
}

// A read-only field that opens a date picker; future dates can't be chosen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    date: LocalDate?,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    var showPicker by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = date?.let { formatDate(it) } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = "Pick date") },
            isError = isError,
            modifier = Modifier.fillMaxWidth()
        )
        // Covers the field so a tap anywhere opens the picker
        Box(Modifier.matchParentSize().clickable { showPicker = true })
    }
    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (date ?: LocalDate.now()).toPickerMillis(),
            selectableDates = PastOrToday
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onDateChange(it.fromPickerMillis()) }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = state, showModeToggle = false)
        }
    }
}

// A read-only field with a menu of options
@Composable
fun <T> DropdownField(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = selected?.let(optionLabel) ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            isError = isError,
            supportingText = supportingText?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        if (enabled) Box(Modifier.matchParentSize().clickable { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ExpenseCategorySelector(
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    DropdownField(
        label = "Expense category",
        options = ExpenseCategories.all,
        selected = selected,
        optionLabel = { it },
        onSelect = onSelect,
        modifier = modifier,
        isError = isError,
        supportingText = if (isError) "Please choose a category" else null
    )
}
