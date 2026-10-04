package com.neoqubix.devajit.h2.data

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.neoqubix.devajit.h2.domain.model.UserProfile
import javax.inject.Inject
import javax.inject.Singleton

/*
 * Tags crash reports with who was using the app, so admin and manager problems can be told apart.
 * Only the user id, role and cart id are sent: never the name, email or phone.
 */
@Singleton
class CrashReporter @Inject constructor(
    private val crashlytics: FirebaseCrashlytics
) {
    fun setUser(user: UserProfile?) {
        crashlytics.setUserId(user?.id.orEmpty())
        crashlytics.setCustomKey("role", user?.role?.value ?: "signed_out")
        crashlytics.setCustomKey("cart_id", user?.cartId.orEmpty())
    }

    // For errors the app recovers from but that shouldn't happen (reported as non-fatal)
    fun record(error: Throwable) {
        crashlytics.recordException(error)
    }
}
