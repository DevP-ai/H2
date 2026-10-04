package com.neoqubix.devajit.h2.data.repository

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/*
 * Firestore keeps offline writes in its local cache and only completes the task once the server has them,
 * so waiting forever would freeze the UI with no internet. Returns true when the server confirmed the write,
 * false when it is still queued (it syncs on its own later). A rejected write throws.
 */
suspend fun Task<Void>.awaitOrQueued(timeoutMs: Long = 8_000L): Boolean =
    withTimeoutOrNull(timeoutMs) { await(); true } ?: false
