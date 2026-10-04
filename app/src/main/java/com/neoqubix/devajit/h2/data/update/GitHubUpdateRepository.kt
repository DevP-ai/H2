package com.neoqubix.devajit.h2.data.update

import android.content.Context
import com.neoqubix.devajit.h2.BuildConfig
import com.neoqubix.devajit.h2.domain.model.AppUpdate
import com.neoqubix.devajit.h2.domain.repository.UpdateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

// Reads update.json from the latest GitHub Release and downloads the APK it points to
@Singleton
class GitHubUpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : UpdateRepository {

    private val updatesDir get() = File(context.cacheDir, "updates").apply { mkdirs() }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true // GitHub redirects release downloads to its file host
            setRequestProperty("Accept", "application/octet-stream, application/json")
            setRequestProperty("Cache-Control", "no-cache")
        }

    override suspend fun fetchLatest(): Result<AppUpdate> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = open(BuildConfig.UPDATE_INFO_URL)
            try {
                if (connection.responseCode == HttpURLConnection.HTTP_NOT_FOUND) error("No release has been published yet.")
                if (connection.responseCode !in 200..299) error("Update server returned ${connection.responseCode}.")
                val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                AppUpdate(
                    versionCode = json.getInt("versionCode"),
                    versionName = json.getString("versionName"),
                    minVersionCode = json.optInt("minVersionCode", 0),
                    apkUrl = json.getString("apkUrl"),
                    sha256 = json.optString("sha256"),
                    releaseNotes = json.optString("releaseNotes")
                ).also { require(it.apkUrl.startsWith("https://github.com/DevP-ai/H2/")) { "Unexpected download location." } }
            } finally {
                connection.disconnect()
            }
        }
    }

    override suspend fun download(update: AppUpdate, onProgress: (Float) -> Unit): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            // Only the newest download is kept
            updatesDir.listFiles()?.forEach { it.delete() }
            val target = File(updatesDir, "h2-v${update.versionName}.apk")
            val partial = File(updatesDir, "${target.name}.part")
            val digest = MessageDigest.getInstance("SHA-256")
            val connection = open(update.apkUrl)
            try {
                if (connection.responseCode !in 200..299) error("Download failed (${connection.responseCode}).")
                val total = connection.contentLengthLong
                var done = 0L
                connection.inputStream.use { input ->
                    partial.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            done += read
                            if (total > 0) onProgress(done.toFloat() / total)
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (update.sha256.isNotBlank() && !actual.equals(update.sha256, ignoreCase = true)) {
                partial.delete()
                error("The download was damaged. Please try again.")
            }
            check(partial.renameTo(target)) { "Couldn't save the update." }
            target
        }
    }
}
