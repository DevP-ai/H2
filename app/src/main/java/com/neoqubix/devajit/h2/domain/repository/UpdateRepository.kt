package com.neoqubix.devajit.h2.domain.repository

import com.neoqubix.devajit.h2.domain.model.AppUpdate
import java.io.File

interface UpdateRepository {
    suspend fun fetchLatest(): Result<AppUpdate>
    // Downloads the APK (progress 0..1) and checks it against the published SHA-256
    suspend fun download(update: AppUpdate, onProgress: (Float) -> Unit): Result<File>
}
