package com.neoqubix.devajit.h2.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.neoqubix.devajit.h2.data.firebase.Collections
import com.neoqubix.devajit.h2.data.firebase.Fields
import com.neoqubix.devajit.h2.domain.model.Role
import com.neoqubix.devajit.h2.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    private val registering = MutableStateFlow(false)
    override val registrationInProgress: StateFlow<Boolean> = registering.asStateFlow()

    override fun observeAuthUid(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        Unit
    }

    override suspend fun register(name: String, email: String, password: String): Result<Unit> {
        registering.value = true
        return try {
            val user = auth.createUserWithEmailAndPassword(email.trim(), password).await().user
                ?: error("Registration failed. Please try again.")
            // Every new account is a manager without a cart; only the Firebase console can make an admin
            val profile = mapOf(
                Fields.NAME to name.trim(),
                Fields.EMAIL to (user.email ?: email.trim()),
                Fields.PHONE to "",
                Fields.ROLE to Role.MANAGER.value,
                Fields.CART_ID to null,
                Fields.CREATED_AT to FieldValue.serverTimestamp(),
                Fields.UPDATED_AT to FieldValue.serverTimestamp()
            )
            try {
                withTimeout(PROFILE_WRITE_TIMEOUT_MS) {
                    firestore.collection(Collections.USERS).document(user.uid).set(profile).await()
                }
            } catch (e: Exception) {
                // Don't leave an account behind that has no profile
                runCatching { user.delete().await() }
                auth.signOut()
                throw e
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            registering.value = false
        }
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }

    override fun logout() {
        auth.signOut()
    }

    private companion object {
        const val PROFILE_WRITE_TIMEOUT_MS = 20_000L
    }
}
