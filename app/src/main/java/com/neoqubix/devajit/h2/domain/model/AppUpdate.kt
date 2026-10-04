package com.neoqubix.devajit.h2.domain.model

// The update.json published with every GitHub Release by .github/workflows/release.yml
data class AppUpdate(
    val versionCode: Int,
    val versionName: String,
    // Installed versions below this must update before using the app
    val minVersionCode: Int,
    val apkUrl: String,
    val sha256: String,
    val releaseNotes: String
) {
    fun isNewerThan(installedCode: Int): Boolean = versionCode > installedCode
    fun isRequiredFor(installedCode: Int): Boolean = installedCode < minVersionCode
}
