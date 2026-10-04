package com.neoqubix.devajit.h2.presentation.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File

// Android 8+ asks the user once to allow this app to install updates
private fun canInstallPackages(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

private fun openInstallPermissionSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

private fun launchInstaller(context: Context, apk: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, "application/vnd.android.package-archive")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

/*
 * Update available -> Download (progress) -> Install (system installer).
 * A required update (installed version below minVersionCode) can't be dismissed; the only way out is to update.
 */
@Composable
fun UpdateDialog(viewModel: UpdateViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val s = state
    if (s is UpdateState.None) return

    val (update, required) = when (s) {
        is UpdateState.Available -> s.update to s.required
        is UpdateState.Downloading -> s.update to s.required
        is UpdateState.ReadyToInstall -> s.update to s.required
        is UpdateState.Failed -> s.update to s.required
        UpdateState.None -> return
    }

    AlertDialog(
        onDismissRequest = { viewModel.dismiss() },
        properties = DialogProperties(dismissOnBackPress = !required, dismissOnClickOutside = !required),
        icon = { Icon(Icons.Outlined.SystemUpdate, contentDescription = null) },
        title = { Text(if (required) "Update required" else "Update available") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Version ${update.versionName} is ready (you have ${viewModel.installedVersionName}).",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (required) {
                    Text("This version is required to keep using the app.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                if (update.releaseNotes.isNotBlank()) {
                    Text("What's new", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(
                        update.releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.heightIn(max = 160.dp).verticalScroll(rememberScrollState())
                    )
                }
                when (s) {
                    is UpdateState.Downloading -> {
                        LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth())
                        Text("Downloading… ${(s.progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                    }
                    is UpdateState.ReadyToInstall -> Text(
                        if (canInstallPackages(context)) "Downloaded. Tap Install, then confirm in the installer."
                        else "Downloaded. Tap Install, allow \"Install unknown apps\" for H2, then come back and tap Install again.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    is UpdateState.Failed -> Text(s.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    else -> Unit
                }
            }
        },
        confirmButton = {
            when (s) {
                is UpdateState.Available -> TextButton(onClick = viewModel::download) { Text("Update now") }
                is UpdateState.Failed -> TextButton(onClick = viewModel::download) { Text("Try again") }
                is UpdateState.Downloading -> TextButton(onClick = {}, enabled = false) { Text("Downloading…") }
                is UpdateState.ReadyToInstall -> TextButton(onClick = {
                    if (!canInstallPackages(context)) openInstallPermissionSettings(context)
                    else runCatching { launchInstaller(context, s.apk) }
                        .onFailure { Toast.makeText(context, "Couldn't open the installer.", Toast.LENGTH_LONG).show() }
                }) { Text("Install") }
                UpdateState.None -> Unit
            }
        },
        dismissButton = {
            if (required) {
                TextButton(onClick = { (context as? Activity)?.finishAffinity() }) { Text("Close app") }
            } else {
                TextButton(onClick = viewModel::dismiss) { Text("Later") }
            }
        }
    )
}
