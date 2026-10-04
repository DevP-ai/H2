package com.neoqubix.devajit.h2.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.SystemUpdate
import com.neoqubix.devajit.h2.BuildConfig
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.neoqubix.devajit.h2.domain.model.UserProfile
import com.neoqubix.devajit.h2.domain.usecase.UpdateProfileUseCase
import com.neoqubix.devajit.h2.utils.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileSaveState(val saving: Boolean = false, val message: String? = null, val isError: Boolean = false)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val updateProfile: UpdateProfileUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileSaveState())
    val state = _state.asStateFlow()

    fun save(uid: String, name: String, phone: String) {
        viewModelScope.launch {
            _state.value = ProfileSaveState(saving = true)
            updateProfile(uid, name, phone)
                .onSuccess { _state.value = ProfileSaveState(message = "Profile saved.") }
                .onFailure { _state.value = ProfileSaveState(message = it.toUserMessage(), isError = true) }
        }
    }
}

// Name and phone are editable; role and cart are shown but only an admin (or the console) can change them
@Composable
fun ProfileScreen(
    user: UserProfile,
    cartName: String?,
    onLogout: () -> Unit,
    onCheckForUpdates: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var name by rememberSaveable(user.id) { mutableStateOf(user.name) }
    var phone by rememberSaveable(user.id) { mutableStateOf(user.phone) }
    var confirmLogout by remember { mutableStateOf(false) }

    LaunchedEffect(user.name, user.phone) {
        name = user.name
        phone = user.phone
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(user.name.ifBlank { "Your profile" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                InfoRow("Email", user.email)
                InfoRow("Role", if (user.isAdmin) "Admin" else "Manager")
                if (!user.isAdmin) InfoRow("Food cart", cartName ?: "Not assigned yet")
                InfoRow("App version", BuildConfig.VERSION_NAME)
            }
        }
        Text("Edit details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            phone, { phone = it },
            label = { Text("Phone (optional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )
        state.message?.let { Text(it, color = if (state.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }
        Button(
            onClick = { viewModel.save(user.id, name, phone) },
            enabled = !state.saving && (name.trim() != user.name || phone.trim() != user.phone),
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (state.saving) "Saving…" else "Save") }
        OutlinedButton(onClick = onCheckForUpdates, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.SystemUpdate, contentDescription = null)
            Text("  Check for updates")
        }
        OutlinedButton(onClick = { confirmLogout = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
            Text("  Log out")
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Log out?") },
            confirmButton = { TextButton(onClick = { confirmLogout = false; onLogout() }) { Text("Log out") } },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row {
        Text("$label: ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
