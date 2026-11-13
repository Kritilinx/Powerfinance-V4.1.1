package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.ui.theme.EmeraldGain
import com.example.ui.theme.RoseLoss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LocalBackupItem(
    val file: File,
    val name: String,
    val displayDate: String,
    val sizeText: String,
    val summary: String,
    val rawJson: String
)

@Composable
fun BackupSettingsScreen(
    onExportJournalsCsv: suspend () -> String,
    onExportLedgerCsv: suspend () -> String,
    onExportBackup: suspend () -> String,
    onImportBackup: suspend (String) -> Result<String>,
    onResetDatabase: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var previewTitle by remember { mutableStateOf<String?>(null) }
    var previewContent by remember { mutableStateOf<String?>(null) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    // List of locally saved backup snapshots
    var localBackups by remember { mutableStateOf<List<LocalBackupItem>>(emptyList()) }
    var isLoadingBackups by remember { mutableStateOf(false) }

    // Dialog & UI states
    var backupToRestore by remember { mutableStateOf<LocalBackupItem?>(null) }
    var showManualPasteDialog by remember { mutableStateOf(false) }
    var manualPasteInput by remember { mutableStateOf("") }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }
    var pendingRestoreDetails by remember { mutableStateOf<String?>(null) }
    var isRestoring by remember { mutableStateOf(false) }

    // State for user custom folder export (SAF)
    var pendingExportJsonForSaf by remember { mutableStateOf<String?>(null) }

    // Load local backups from disk
    fun refreshLocalBackups() {
        scope.launch(Dispatchers.IO) {
            isLoadingBackups = true
            val backupsDir = File(context.filesDir, "backups")
            if (!backupsDir.exists()) {
                backupsDir.mkdirs()
            }
            val files = backupsDir.listFiles { f -> f.extension.equals("json", ignoreCase = true) }
                ?.sortedByDescending { it.lastModified() } ?: emptyList()

            val items = files.mapNotNull { file ->
                try {
                    val content = file.readText()
                    val root = JSONObject(content)
                    val accCount = root.optJSONArray("accounts")?.length() ?: 0
                    val txCount = root.optJSONArray("transactions")?.length() ?: 0
                    val jeCount = root.optJSONArray("journal_entries")?.length() ?: 0
                    val invCount = root.optJSONArray("inventory_logs")?.length() ?: 0

                    val sizeKb = (file.length() / 1024.0).coerceAtLeast(0.1)
                    val dateFormatted = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
                        .format(Date(file.lastModified()))

                    LocalBackupItem(
                        file = file,
                        name = file.name,
                        displayDate = dateFormatted,
                        sizeText = String.format(Locale.US, "%.1f KB", sizeKb),
                        summary = "$accCount Akun • $txCount Transaksi • $jeCount Jurnal",
                        rawJson = content
                    )
                } catch (e: Exception) {
                    null
                }
            }

            withContext(Dispatchers.Main) {
                localBackups = items
                isLoadingBackups = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshLocalBackups()
    }

    // Helper: Save directly to Android Downloads folder
    fun saveToAndroidDownloads(json: String, fileName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DiprintAccounting")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                    true
                } else false
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloadsDir, "DiprintAccounting")
                if (!targetDir.exists()) targetDir.mkdirs()
                val targetFile = File(targetDir, fileName)
                targetFile.writeText(json)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    // Helper: Share file cleanly using FileProvider
    fun shareBackup(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, "Bagikan Berkas Cadangan (${file.name})")
            context.startActivity(chooser)
        } catch (e: Exception) {
            // Fallback plain share if needed
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, file.readText())
                putExtra(Intent.EXTRA_SUBJECT, file.name)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Bagikan Teks Cadangan"))
        }
    }

    // File picker launcher for opening any backup file from Downloads, Drive, WhatsApp, etc.
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val content = stream.bufferedReader().use { reader -> reader.readText() }
                    try {
                        val root = JSONObject(content)
                        if (!root.has("appName") || !root.getString("appName").contains("Diprint", ignoreCase = true)) {
                            Toast.makeText(context, "Berkas bukan format cadangan Diprint Accounting yang valid", Toast.LENGTH_LONG).show()
                            return@let
                        }
                        val accCount = root.optJSONArray("accounts")?.length() ?: 0
                        val txCount = root.optJSONArray("transactions")?.length() ?: 0
                        val jeCount = root.optJSONArray("journal_entries")?.length() ?: 0

                        pendingRestoreJson = content
                        pendingRestoreDetails = "Berkas Pilihan Anda ($accCount Akun, $txCount Transaksi, $jeCount Jurnal)"
                    } catch (e: Exception) {
                        Toast.makeText(context, "Isi berkas tidak valid: format JSON rusak", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal membuka berkas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // SAF Create Document launcher for saving file to user-selected folder
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { targetUri ->
            val json = pendingExportJsonForSaf
            if (json != null) {
                try {
                    context.contentResolver.openOutputStream(targetUri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, "Berkas cadangan berhasil disimpan di lokasi pilihan Anda", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal menyimpan berkas: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    pendingExportJsonForSaf = null
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // App Identity Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Diprint Accounting",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Pusat Cadangan & Pemulihan Data. Semua transaksi & akun Anda dapat dicadangkan dengan 1-sentuhan dan dipulihkan kapan saja secara aman.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // 1. EASY 1-TAP BACKUP SECTION
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_backup_restore"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Backup, contentDescription = null, tint = EmeraldGain)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cadangkan Data (Backup)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Simpan seluruh catatan pembukuan, bagan akun, jurnal, dan saldo Anda agar tidak hilang saat ganti perangkat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary Button: 1-Tap Auto Backup
                    Button(
                        onClick = {
                            scope.launch {
                                val json = onExportBackup()
                                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                val fileName = "Diprint_Backup_$timeStamp.json"

                                // 1. Save to internal backups directory
                                val backupsDir = File(context.filesDir, "backups")
                                if (!backupsDir.exists()) backupsDir.mkdirs()
                                val localFile = File(backupsDir, fileName)
                                localFile.writeText(json)

                                // 2. Also save to public Downloads folder so users can find it in phone file manager
                                val savedToDownloads = saveToAndroidDownloads(json, fileName)

                                refreshLocalBackups()

                                val msg = if (savedToDownloads) {
                                    "✓ Cadangan berhasil dibuat & disimpan di folder Download/DiprintAccounting!"
                                } else {
                                    "✓ Cadangan berhasil dibuat di memori perangkat ini!"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_export_full_backup"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGain)
                    ) {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cadangkan Data Sekarang (1-Sentuhan)", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Secondary Option: Choose Custom Folder (SAF)
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val json = onExportBackup()
                                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                pendingExportJsonForSaf = json
                                createDocumentLauncher.launch("Diprint_Backup_$timeStamp.json")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pilih Folder Penyimpanan Sendiri (Download/SD Card)")
                    }
                }
            }
        }

        // 2. SAVED BACKUPS ON THIS DEVICE (EASY 1-CLICK RESTORE)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cadangan Tersimpan di HP Anda",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { refreshLocalBackups() }) {
                            Icon(imageVector = Icons.Default.History, contentDescription = "Muat Ulang", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Text(
                        text = "Daftar cadangan yang pernah Anda buat di HP ini. Cukup tekan tombol 'Pulihkan' untuk mengembalikan data secara instan tanpa perlu mencari berkas manual.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (localBackups.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Belum Ada Cadangan di HP Ini",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tekan tombol 'Cadangkan Data Sekarang' di atas untuk menyimpan salinan cadangan pertama Anda.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            localBackups.forEach { backup ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(EmeraldGain.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = EmeraldGain, modifier = Modifier.size(18.dp))
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = backup.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "${backup.displayDate} • ${backup.sizeText}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    backup.file.delete()
                                                    refreshLocalBackups()
                                                    Toast.makeText(context, "Cadangan dihapus", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = backup.summary,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = EmeraldGain,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            // 1-Tap Restore Button
                                            Button(
                                                onClick = {
                                                    pendingRestoreJson = backup.rawJson
                                                    pendingRestoreDetails = "${backup.name} (${backup.summary})"
                                                },
                                                modifier = Modifier.weight(1.3f),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                            ) {
                                                Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Pulihkan Data", style = MaterialTheme.typography.labelLarge)
                                            }

                                            // Share file button
                                            OutlinedButton(
                                                onClick = { shareBackup(backup.file) },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Kirim File")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. IMPORT FROM EXTERNAL FILE OR PASTE TEXT
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.FileOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Impor Berkas Cadangan dari Luar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Gunakan opsi ini jika Anda menerima berkas cadangan dari HP lain, kiriman WhatsApp, atau menyalin teks cadangan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Button: Pick File from Phone
                    Button(
                        onClick = { openDocumentLauncher.launch(arrayOf("*/*", "application/json", "text/*")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_import_full_backup"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pilih Berkas Cadangan dari HP (Download/WA/Drive)")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Button: Paste Text Manually
                    OutlinedButton(
                        onClick = {
                            manualPasteInput = ""
                            showManualPasteDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tempel Kode Teks Cadangan (Salin / Tempel)")
                    }
                }
            }
        }

        // 4. CSV Export Section (Reports)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ekspor Berkas CSV (Spreadsheet)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ekspor data ke berkas CSV terpisah untuk Jurnal Umum dan Buku Besar agar dapat dibuka di Microsoft Excel atau Google Sheets.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Button 1: Export Journals CSV
                    Button(
                        onClick = {
                            scope.launch {
                                val csv = onExportJournalsCsv()
                                previewTitle = "Ekspor Jurnal Umum (CSV)"
                                previewContent = csv
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_export_journals_csv"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ekspor Jurnal Umum ke CSV")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Button 2: Export Ledger CSV
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val csv = onExportLedgerCsv()
                                previewTitle = "Ekspor Buku Besar (CSV)"
                                previewContent = csv
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_export_ledger_csv"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ekspor Buku Besar ke CSV")
                    }
                }
            }
        }

        // 5. System Features
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Karakteristik & Integritas Sistem",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    SystemFeatureItem("100% Offline-First", "Seluruh kalkulasi dan data tersimpan di penyimpanan internal SQLite Room tanpa ketergantungan internet.")
                    SystemFeatureItem("Double-Entry Bookkeeping", "Setiap transaksi otomatis membuat jurnal berpasangan di mana Total Debit selalu sama dengan Total Kredit.")
                    SystemFeatureItem("Cadangan Aman & Portabel", "Fitur Ekspor & Impor memungkinkan backup data ke file JSON dan pemulihan kapan saja dengan validasi integritas.")
                    SystemFeatureItem("Audit Trail Terlindungi", "Transaksi diposting tidak dihapus langsung, melainkan dibatalkan dengan jurnal pembalik.")
                    SystemFeatureItem("Donasi Pemilik Khusus", "Donasi mengurangi ekuitas pemilik (Prive Donasi), tidak dimasukkan ke dalam beban operasional.")
                    SystemFeatureItem("Persediaan Nilai Total", "Mengelola nilai aset persediaan rupiah tanpa beban SKU atau kuantitas unit.")
                }
            }
        }

        // 6. Danger Zone: Reset Database
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Zona Pengaturan Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = RoseLoss
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Reset database akan membersihkan riwayat transaksi tambahan dan mengembalikan database ke saldo awal resmi per 31 Oktober 2026 (Rp244.910,94).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showResetConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = RoseLoss),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_reset_database"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset ke Saldo Awal (31 Okt 2026)")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(30.dp)) }
    }

    // ==========================================
    // DIALOG: CONFIRM RESTORE BACKUP (CLEAR & USER-FRIENDLY)
    // ==========================================
    if (pendingRestoreJson != null) {
        val jsonToRestore = pendingRestoreJson!!
        val detailsText = pendingRestoreDetails ?: "Berkas Cadangan Terpilih"

        AlertDialog(
            onDismissRequest = { if (!isRestoring) pendingRestoreJson = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pulihkan Cadangan Data?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Anda akan memulihkan data dari:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = detailsText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Semua transaksi, akun, dan jurnal saat ini akan diperbarui sesuai isi cadangan secara utuh.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isRestoring = true
                            val result = onImportBackup(jsonToRestore)
                            isRestoring = false
                            pendingRestoreJson = null
                            pendingRestoreDetails = null
                            if (result.isSuccess) {
                                refreshLocalBackups()
                                Toast.makeText(context, "✓ Data cadangan berhasil dipulihkan secara utuh!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Gagal memulihkan: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_restore_action")
                ) {
                    Text(if (isRestoring) "Memproses..." else "Ya, Pulihkan Sekarang")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingRestoreJson = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // ==========================================
    // DIALOG: MANUAL PASTE TEXT
    // ==========================================
    if (showManualPasteDialog) {
        AlertDialog(
            onDismissRequest = { showManualPasteDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tempel Kode Teks Cadangan", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Tempel teks cadangan JSON yang Anda salin dari WhatsApp, Email, atau catatan:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = manualPasteInput,
                        onValueChange = { manualPasteInput = it },
                        label = { Text("Teks Cadangan (JSON)") },
                        placeholder = { Text("{\"appName\":\"Diprint Accounting\", ...}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .testTag("input_import_backup_json"),
                        maxLines = 8
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    val text = clip.getItemAt(0).text?.toString() ?: ""
                                    manualPasteInput = text
                                    Toast.makeText(context, "Teks dari clipboard ditempel", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tempel dari Papan Klip (Clipboard)")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualPasteInput.isBlank()) {
                            Toast.makeText(context, "Teks cadangan belum dimasukkan", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        try {
                            val root = JSONObject(manualPasteInput)
                            val accCount = root.optJSONArray("accounts")?.length() ?: 0
                            val txCount = root.optJSONArray("transactions")?.length() ?: 0

                            showManualPasteDialog = false
                            pendingRestoreJson = manualPasteInput
                            pendingRestoreDetails = "Teks Cadangan ($accCount Akun, $txCount Transaksi)"
                        } catch (e: Exception) {
                            Toast.makeText(context, "Format teks cadangan tidak valid: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Text("Lanjutkan Pemulihan")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showManualPasteDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // ==========================================
    // DIALOG: CSV PREVIEW & SHARE DIALOG
    // ==========================================
    if (previewTitle != null && previewContent != null) {
        val title = previewTitle!!
        val content = previewContent!!

        AlertDialog(
            onDismissRequest = {
                previewTitle = null
                previewContent = null
            },
            title = { Text(title, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Pratinjau Berkas CSV (Dapat disalin atau dibagikan ke aplikasi spreadsheet):",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        LazyColumn(modifier = Modifier.padding(10.dp)) {
                            item {
                                Text(
                                    text = content,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_TEXT, content)
                            putExtra(Intent.EXTRA_SUBJECT, title)
                        }
                        val shareIntent = Intent.createChooser(sendIntent, title)
                        context.startActivity(shareIntent)
                    }
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bagikan / Simpan")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Diprint CSV", content)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "CSV berhasil disalin ke clipboard", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Salin")
                    }
                    OutlinedButton(
                        onClick = {
                            previewTitle = null
                            previewContent = null
                        }
                    ) {
                        Text("Tutup")
                    }
                }
            }
        )
    }

    // ==========================================
    // DIALOG: RESET CONFIRMATION DIALOG
    // ==========================================
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Konfirmasi Reset Database", fontWeight = FontWeight.Bold, color = RoseLoss) },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin mengatur ulang data? Semua transaksi baru akan dibersihkan dan saldo awal per 31 Oktober 2026 (Rp244.910,94) akan dipulihkan secara bersih.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetDatabase()
                        showResetConfirmDialog = false
                        refreshLocalBackups()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseLoss),
                    modifier = Modifier.testTag("btn_confirm_reset_action")
                ) {
                    Text("Ya, Reset Database")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun SystemFeatureItem(title: String, description: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = "• $title", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
