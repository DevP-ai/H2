package com.neoqubix.devajit.h2.domain.usecase

import com.neoqubix.devajit.h2.domain.model.UserProfile
import com.neoqubix.devajit.h2.domain.repository.AuthRepository
import com.neoqubix.devajit.h2.domain.repository.UserRepository
import com.neoqubix.devajit.h2.utils.toUserMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

sealed interface Session {
    data object Loading : Session
    data object SignedOut : Session
    // Signed in, but users/{uid} doesn't exist
    data object MissingProfile : Session
    data class Error(val message: String) : Session
    data class Ready(val user: UserProfile) : Session
}

private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

fun isValidEmail(email: String) = EMAIL_REGEX.matches(email.trim())

// Who is signed in and their live profile (role and cart changes made by an admin apply immediately)
class ObserveSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Session> =
        combine(authRepository.observeAuthUid(), authRepository.registrationInProgress) { uid, registering -> uid to registering }
            .distinctUntilChanged()
            .flatMapLatest { (uid, registering) ->
                when {
                    uid == null -> flowOf(Session.SignedOut)
                    registering -> flowOf(Session.Loading)
                    else -> userRepository.observeUser(uid)
                        .map { profile -> if (profile == null) Session.MissingProfile else Session.Ready(profile) }
                        .catch { emit(Session.Error(it.toUserMessage())) }
                }
            }
}

class LoginUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        if (email.isBlank() || password.isBlank()) return Result.failure(IllegalArgumentException("Please enter your email and password."))
        if (!isValidEmail(email)) return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        return authRepository.login(email, password)
    }
}

class RegisterUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(name: String, email: String, password: String, confirmPassword: String): Result<Unit> {
        val error = when {
            name.isBlank() -> "Please enter your name."
            name.trim().length > 80 -> "Name is too long."
            !isValidEmail(email) -> "Please enter a valid email address."
            password.length < 6 -> "Password must be at least 6 characters."
            password != confirmPassword -> "Passwords do not match."
            else -> null
        }
        if (error != null) return Result.failure(IllegalArgumentException(error))
        // The role is always "manager"; it is not an input
        return authRepository.register(name, email, password)
    }
}

class ResetPasswordUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String): Result<Unit> {
        if (!isValidEmail(email)) return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        return authRepository.sendPasswordReset(email)
    }
}

class LogoutUseCase @Inject constructor(private val authRepository: AuthRepository) {
    operator fun invoke() = authRepository.logout()
}

class UpdateProfileUseCase @Inject constructor(private val userRepository: UserRepository) {
    suspend operator fun invoke(uid: String, name: String, phone: String): Result<Unit> {
        if (name.isBlank()) return Result.failure(IllegalArgumentException("Please enter your name."))
        if (phone.isNotBlank() && !phone.trim().matches(Regex("^[+0-9 ]{7,15}$"))) {
            return Result.failure(IllegalArgumentException("Please enter a valid phone number."))
        }
        return userRepository.updateOwnProfile(uid, name, phone)
    }
}
