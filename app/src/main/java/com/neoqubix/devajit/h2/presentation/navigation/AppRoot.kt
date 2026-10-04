package com.neoqubix.devajit.h2.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neoqubix.devajit.h2.domain.model.Role
import com.neoqubix.devajit.h2.domain.usecase.Session
import com.neoqubix.devajit.h2.presentation.auth.AuthNavHost
import com.neoqubix.devajit.h2.presentation.auth.SessionViewModel
import com.neoqubix.devajit.h2.presentation.auth.SplashScreen
import com.neoqubix.devajit.h2.presentation.components.ErrorView

/*
 * Splash -> signed in? -> profile -> role -> Admin or Manager.
 * The splash stays up until the profile is known, so a signed-in user never sees Login flash by.
 * A role or cart change made by an admin switches the screens immediately.
 */
@Composable
fun AppRoot(viewModel: SessionViewModel = hiltViewModel()) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.syncErrors.collect { snackbar.showSnackbar(it) }
    }

    LaunchedEffect(session !is Session.Ready) {
        if (session !is Session.Ready) viewModel.endSession()
    }

    Box(Modifier.fillMaxSize()) {
        when (val s = session) {
            Session.Loading -> SplashScreen()
            Session.SignedOut -> AuthNavHost()
            Session.MissingProfile -> NoCartScreen(
                title = "Your profile could not be found.",
                message = "Please contact the administrator, or log out and register again.",
                onLogout = viewModel::logout
            )
            is Session.Error -> Box(Modifier.fillMaxSize().systemBarsPadding(), contentAlignment = Alignment.Center) {
                ErrorView(s.message, onRetry = viewModel::retry)
                OutlinedButton(onClick = viewModel::logout, modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp)) { Text("Log out") }
            }
            is Session.Ready -> key(s.user.id, s.user.role) {
                SessionViewModelScope(viewModel.storeFor(s.user.id to s.user.role)) {
                    if (s.user.role == Role.ADMIN) AdminRoot(s.user, viewModel::logout)
                    else ManagerRoot(s.user, viewModel::logout)
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).systemBarsPadding().padding(bottom = 80.dp))
    }
}
