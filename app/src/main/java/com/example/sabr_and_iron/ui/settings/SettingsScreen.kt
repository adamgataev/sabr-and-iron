package com.example.sabr_and_iron.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sabr_and_iron.ui.components.ConfirmDeleteDialog
import com.example.sabr_and_iron.ui.components.FloatingIslandOnlyListBottomPadding
import com.example.sabr_and_iron.ui.components.LocalAppSnackbar
import com.example.sabr_and_iron.ui.records.RecordsViewModel
import java.io.File

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: RecordsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = LocalAppSnackbar.current
    var confirmClear by remember { mutableStateOf(false) }
    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { viewModel.importJson(it) }
    }
    val importExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { viewModel.importExcel(it) }
    }

    fun shareFile(file: File, mime: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share export"))
    }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = FloatingIslandOnlyListBottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Data", style = MaterialTheme.typography.titleMedium)
        Text(
            "Backup, restore, or reset everything stored on this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = { viewModel.exportJson { shareFile(it, "application/json") } },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Export JSON")
        }
        OutlinedButton(
            onClick = {
                importJsonLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Import JSON")
        }
        OutlinedButton(
            onClick = {
                viewModel.exportExcel {
                    shareFile(
                        it,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Export Excel")
        }
        OutlinedButton(
            onClick = {
                importExcelLauncher.launch(
                    arrayOf(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-excel",
                        "*/*",
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Import Excel")
        }
        TextButton(
            onClick = { confirmClear = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Clear all data", color = MaterialTheme.colorScheme.error)
        }
    }

    if (confirmClear) {
        ConfirmDeleteDialog(
            title = "Clear all data?",
            body = "This deletes workouts, routines, and exercises stored on this device.",
            confirmLabel = "Clear all",
            onConfirm = {
                confirmClear = false
                viewModel.clearAllData()
            },
            onDismiss = { confirmClear = false },
        )
    }
}
