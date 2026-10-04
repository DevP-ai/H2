package com.neoqubix.devajit.h2.data.update

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// Remembers the optional update the user chose "Later" for, so the launch check doesn't ask again for it
@Singleton
class UpdatePreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("app_update", Context.MODE_PRIVATE)

    var skippedVersionCode: Int
        get() = prefs.getInt(KEY_SKIPPED, 0)
        set(value) = prefs.edit().putInt(KEY_SKIPPED, value).apply()

    private companion object {
        const val KEY_SKIPPED = "skipped_version_code"
    }
}
