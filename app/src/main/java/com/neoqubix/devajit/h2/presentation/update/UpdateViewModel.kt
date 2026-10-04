package com.neoqubix.devajit.h2.presentation.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neoqubix.devajit.h2.BuildConfig
import com.neoqubix.devajit.h2.domain.model.AppUpdate
import com.neoqubix.devajit.h2.domain.repository.UpdateRepository
import com.neoqubix.devajit.h2.utils.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface UpdateState {
    data object None : UpdateState
    data class Available(val update: AppUpdate, val required: Boolean) : UpdateState
    data class Downloading(val update: AppUpdate, val required: Boolean, val progress: Float) : UpdateState
    data class ReadyToInstall(val update: AppUpdate, val required: Boolean, val apk: File) : UpdateState
    data class Failed(val update: AppUpdate, val required: Boolean, val message: String) : UpdateState
}

// Lives for the whole app (not per login), so a required update blocks every screen including Login
@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val repository: UpdateRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UpdateState>(UpdateState.None)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 2)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val installedVersionName: String = BuildConfig.VERSION_NAME
    val installedVersionCode: Int = BuildConfig.VERSION_CODE

    private var checkJob: Job? = null
    private var downloadJob: Job? = null
    private var checkedOnLaunch = false

    // Silent check when the app starts. Debug builds skip it: their signature differs from the release builds.
    fun checkOnLaunch() {
        if (checkedOnLaunch || BuildConfig.DEBUG) return
        checkedOnLaunch = true
        check(manual = false)
    }

    fun check(manual: Boolean = true) {
        if (checkJob?.isActive == true || downloadJob?.isActive == true) return
        checkJob = viewModelScope.launch {
            repository.fetchLatest()
                .onSuccess { update ->
                    if (update.isNewerThan(installedVersionCode)) {
                        _state.value = UpdateState.Available(update, update.isRequiredFor(installedVersionCode))
                    } else if (manual) {
                        _messages.tryEmit("You have the latest version ($installedVersionName).")
                    }
                }
                .onFailure { if (manual) _messages.tryEmit("Couldn't check for updates. ${it.toUserMessage()}") }
        }
    }

    fun download() {
        val (update, required) = when (val s = _state.value) {
            is UpdateState.Available -> s.update to s.required
            is UpdateState.Failed -> s.update to s.required
            else -> return
        }
        downloadJob = viewModelScope.launch {
            _state.value = UpdateState.Downloading(update, required, 0f)
            repository.download(update) { p ->
                _state.update { if (it is UpdateState.Downloading) it.copy(progress = p) else it }
            }
                .onSuccess { _state.value = UpdateState.ReadyToInstall(update, required, it) }
                .onFailure { _state.value = UpdateState.Failed(update, required, it.toUserMessage()) }
        }
    }

    // "Later" is only possible for optional updates
    fun dismiss() {
        val required = when (val s = _state.value) {
            is UpdateState.Available -> s.required
            is UpdateState.Downloading -> s.required
            is UpdateState.ReadyToInstall -> s.required
            is UpdateState.Failed -> s.required
            UpdateState.None -> false
        }
        if (required) return
        downloadJob?.cancel()
        _state.value = UpdateState.None
    }
}
