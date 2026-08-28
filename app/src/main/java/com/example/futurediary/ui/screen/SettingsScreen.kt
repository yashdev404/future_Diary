package com.example.futurediary.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.futurediary.ui.util.JournalPdfExporter
import com.example.futurediary.ui.util.SecurityManager
import com.example.futurediary.ui.viewmodel.DiaryViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: DiaryViewModel,
    onOpenDrawer: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    
    val securityManager = remember { SecurityManager(context) }
    var biometricEnabled by remember { mutableStateOf(securityManager.isBiometricEnabled) }
    var companionEnabled by remember { mutableStateOf(securityManager.isCompanionEnabled) }
    var cloudSyncEnabled by remember { mutableStateOf(securityManager.isCloudSyncEnabled) }
    
    var showDateRangePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()
    
    var isExporting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                "Data & Export",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Export Memories as PDF", style = MaterialTheme.typography.titleMedium)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val selectedRangeText = if (dateRangePickerState.selectedStartDateMillis != null && dateRangePickerState.selectedEndDateMillis != null) {
                        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        "${sdf.format(Date(dateRangePickerState.selectedStartDateMillis!!))} - ${sdf.format(Date(dateRangePickerState.selectedEndDateMillis!!))}"
                    } else {
                        "Current Month (Default)"
                    }
                    
                    OutlinedButton(
                        onClick = { showDateRangePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(selectedRangeText)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            scope.launch {
                                isExporting = true
                                
                                val start = dateRangePickerState.selectedStartDateMillis ?: getFirstDayOfMonth()
                                val end = dateRangePickerState.selectedEndDateMillis ?: getLastDayOfMonth()
                                
                                // Filter entries by selected range
                                val filteredEntries = entries.filter { 
                                    it.entry.date in start..end
                                }.sortedByDescending { it.entry.date }

                                if (filteredEntries.isEmpty()) {
                                    Toast.makeText(context, "No memories found in this range", Toast.LENGTH_SHORT).show()
                                } else {
                                    val file = JournalPdfExporter.exportToPdf(context, filteredEntries)
                                    if (file != null) {
                                        Toast.makeText(context, "Exported: ${file.name}", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                isExporting = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isExporting && entries.isNotEmpty()
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Download PDF")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Security",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("App Lock", style = MaterialTheme.typography.titleMedium)
                            Text("Protect diary with biometrics", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = { 
                            if (it) {
                                if (securityManager.canUseBiometrics()) {
                                    biometricEnabled = true
                                    securityManager.isBiometricEnabled = true
                                } else {
                                    Toast.makeText(context, "Biometrics not available on this device", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                biometricEnabled = false
                                securityManager.isBiometricEnabled = false
                            }
                        }
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Emotional Companion", style = MaterialTheme.typography.titleMedium)
                            Text("Show happy memories in sad times", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Switch(
                        checked = companionEnabled,
                        onCheckedChange = { 
                            companionEnabled = it
                            securityManager.isCompanionEnabled = it
                        }
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Cloud Backup", style = MaterialTheme.typography.titleMedium)
                            Text("Mirror memories to private cloud", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Switch(
                        checked = cloudSyncEnabled,
                        onCheckedChange = { 
                            cloudSyncEnabled = it
                            securityManager.isCloudSyncEnabled = it
                            if (it) {
                                val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.futurediary.data.sync.SyncWorker>().build()
                                androidx.work.WorkManager.getInstance(context).enqueue(syncRequest)
                            }
                        }
                    )
                }
            }
        }

        if (showDateRangePicker) {
            DatePickerDialog(
                onDismissRequest = { showDateRangePicker = false },
                confirmButton = {
                    TextButton(onClick = { showDateRangePicker = false }) { Text("Done") }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        dateRangePickerState.setSelection(null, null)
                        showDateRangePicker = false 
                    }) { Text("Clear") }
                }
            ) {
                DateRangePicker(
                    state = dateRangePickerState,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun getFirstDayOfMonth(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun getLastDayOfMonth(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 999)
    return cal.timeInMillis
}
