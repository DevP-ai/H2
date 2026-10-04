package com.neoqubix.devajit.h2.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

/*
 * Gives everything inside (the admin or manager screens, their NavHost and its back stack) a ViewModelStore that
 * belongs to one signed-in session. The store is cleared at logout or when the user/role changes, which stops
 * every Firestore listener of the old session before its access is revoked. It survives rotation.
 */
@Composable
fun SessionViewModelScope(store: ViewModelStore, content: @Composable () -> Unit) {
    val parent = checkNotNull(LocalViewModelStoreOwner.current) { "No ViewModelStoreOwner" }
    val owner = remember(store, parent) { SessionStoreOwner(store, parent) }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner, content = content)
}

// Uses the activity's (Hilt) factory, so hiltViewModel() works the same inside the scope
private class SessionStoreOwner(
    override val viewModelStore: ViewModelStore,
    private val parent: ViewModelStoreOwner
) : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = (parent as HasDefaultViewModelProviderFactory).defaultViewModelProviderFactory
    override val defaultViewModelCreationExtras: CreationExtras
        get() = (parent as HasDefaultViewModelProviderFactory).defaultViewModelCreationExtras
}
