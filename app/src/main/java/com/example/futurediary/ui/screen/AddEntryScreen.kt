package com.example.futurediary.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.futurediary.ui.viewmodel.DiaryViewModel
import com.example.futurediary.ui.util.TemplateRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
    viewModel: DiaryViewModel,
    onNavigateBack: () -> Unit,
    entryId: Long = -1L
) {
    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var selectedImages by rememberSaveable { mutableStateOf<List<Uri>>(emptyList()) }
    var isInitialized by rememberSaveable { mutableStateOf(false) }
    var currentId by rememberSaveable { mutableStateOf(if (entryId == -1L) 0L else entryId) }
    var isAutoSaving by remember { mutableStateOf(false) }
    
    var isVaultItem by rememberSaveable { mutableStateOf(false) }
    var unlockDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var showUnlockDatePicker by remember { mutableStateOf(false) }
    
    var showTemplateMenu by remember { mutableStateOf(false) }
    
    val contentFocusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    val entryToEdit by if (entryId != -1L) {
        viewModel.getEntryById(entryId).collectAsStateWithLifecycle(initialValue = null)
    } else {
        remember { mutableStateOf(null) }
    }

    LaunchedEffect(entryToEdit) {
        if (entryToEdit != null && !isInitialized) {
            val wrap = entryToEdit!!
            title = wrap.entry.title
            content = wrap.entry.content
            selectedImages = wrap.images.map { it.imageUri.toUri() }
            isVaultItem = wrap.entry.isVaultItem
            unlockDate = wrap.entry.unlockDate
            isInitialized = true
        }
    }

    if (showUnlockDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showUnlockDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    unlockDate = datePickerState.selectedDateMillis
                    showUnlockDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Auto-save logic
    LaunchedEffect(title, content, selectedImages) {
        if (title.isBlank() && content.isBlank() && selectedImages.isEmpty()) return@LaunchedEffect
        
        delay(2000)
        isAutoSaving = true
        val newId = viewModel.saveEntry(
            title = title,
            content = content,
            images = selectedImages,
            id = currentId,
            isDraft = true
        )
        currentId = newId
        isAutoSaving = false
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris -> 
            selectedImages = (selectedImages + uris).distinct()
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (entryId == -1L) "New Memory" else "Edit Memory") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Add Photo")
                    }

                    Box {
                        IconButton(onClick = { showTemplateMenu = true }) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = "Use Template")
                        }
                        DropdownMenu(
                            expanded = showTemplateMenu,
                            onDismissRequest = { showTemplateMenu = false }
                        ) {
                            TemplateRegistry.templates.forEach { template ->
                                DropdownMenuItem(
                                    text = { Text(template.name) },
                                    onClick = {
                                        content = if (content.isBlank()) template.content else "$content\n\n${template.content}"
                                        showTemplateMenu = false
                                    }
                                )
                            }
                        }
                    }

                    if (isAutoSaving) {
                        Icon(
                            Icons.Default.CloudSync, 
                            contentDescription = "Saving...",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    } else if (title.isNotBlank() || content.isNotBlank() || selectedImages.isNotEmpty()) {
                        Icon(
                            Icons.Default.CloudDone, 
                            contentDescription = "Saved",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { contentFocusRequester.requestFocus() }
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Photo Ribbon
            if (selectedImages.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedImages.size) { index ->
                        val uri = selectedImages[index]
                        Box {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { selectedImages = selectedImages.filter { it != uri } },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(MaterialTheme.colorScheme.error, CircleShape)
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close, 
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("What happened today?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp)
                    .focusRequester(contentFocusRequester)
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isVaultItem,
                    onCheckedChange = { isVaultItem = it }
                )
                Text("Lock this in the Memory Vault")
            }

            if (isVaultItem) {
                TextButton(onClick = { showUnlockDatePicker = true }) {
                    val dateFormatter = remember { java.text.SimpleDateFormat("MMM dd, yyyy") }
                    val label = if (unlockDate != null) {
                        "Unlocks on: " + dateFormatter.format(java.util.Date(unlockDate!!))
                    } else {
                        "Pick Unlock Date"
                    }
                    Text(label)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        scope.launch {
                            viewModel.saveEntry(
                                title = title,
                                content = content,
                                images = selectedImages,
                                id = currentId,
                                isDraft = false,
                                isVaultItem = isVaultItem,
                                unlockDate = unlockDate
                            )
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && content.isNotBlank()
            ) {
                Text(if (entryId == -1L) "Save to Future" else "Update Memory")
            }
        }
    }
}
