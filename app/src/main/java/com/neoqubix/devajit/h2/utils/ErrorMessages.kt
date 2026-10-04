package com.neoqubix.devajit.h2.utils

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.TimeoutCancellationException

// Short, plain messages for managers; never raw exception text from Firebase
fun Throwable.toUserMessage(): String = when (this) {
    is FirebaseNetworkException, is TimeoutCancellationException ->
        "No internet connection. Please check your connection and try again."
    is FirebaseTooManyRequestsException -> "Too many attempts. Please wait a few minutes and try again."
    is FirebaseAuthWeakPasswordException -> "Password is too weak. Use at least 6 characters."
    is FirebaseAuthUserCollisionException -> "An account with this email already exists. Please log in."
    is FirebaseAuthInvalidUserException,
    is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
    is FirebaseFirestoreException -> when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> "You don't have permission to do this."
        FirebaseFirestoreException.Code.UNAVAILABLE -> "No internet connection. Please check your connection and try again."
        FirebaseFirestoreException.Code.NOT_FOUND -> "This item no longer exists."
        FirebaseFirestoreException.Code.FAILED_PRECONDITION -> "The database is still being set up. Please try again in a few minutes."
        else -> "Something went wrong. Please try again."
    }
    is IllegalStateException, is IllegalArgumentException -> message ?: "Something went wrong. Please try again."
    else -> "Something went wrong. Please try again."
}
