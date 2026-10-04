package com.neoqubix.devajit.h2.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.neoqubix.devajit.h2.domain.repository.TransactionRepository
import com.neoqubix.devajit.h2.domain.usecase.LogoutUseCase
import com.neoqubix.devajit.h2.domain.usecase.ObserveSessionUseCase
import com.neoqubix.devajit.h2.domain.usecase.Session
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// Decides what the app shows: splash, login, or the dashboard for the user's role
@HiltViewModel
class SessionViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val logoutUseCase: LogoutUseCase,
    transactionRepository: TransactionRepository
) : ViewModel() {

    private val retry = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val session: StateFlow<Session> = retry
        .flatMapLatest { observeSession() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Session.Loading)

    val syncErrors: SharedFlow<String> = transactionRepository.syncErrors

    // ViewModels of the current session (see SessionViewModelScope); replaced when the user or role changes
    private var sessionKey: Any? = null
    private var sessionStore: ViewModelStore? = null

    fun storeFor(key: Any): ViewModelStore {
        if (key != sessionKey) endSession()
        return sessionStore ?: ViewModelStore().also {
            sessionStore = it
            sessionKey = key
        }
    }

    // Clears the old session's ViewModels, which closes their Firestore listeners
    fun endSession() {
        sessionStore?.clear()
        sessionStore = null
        sessionKey = null
    }

    override fun onCleared() {
        endSession()
    }

    fun retry() {
        retry.value++
    }

    fun logout() {
        // Stop listening before access is revoked, so no listener is rejected mid-flight
        endSession()
        logoutUseCase()
    }
}
