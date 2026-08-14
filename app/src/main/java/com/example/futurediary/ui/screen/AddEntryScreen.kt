package com.example.futurediary.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.futurediary.ui.viewmodel.DiaryViewModel
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
    var selectedImageUri by rememberSaveable { mutableStateOf<android.net.Uri?>(null) }
    var isInitialized by rememberSaveable { mutableStateOf(false) }
    var currentId by rememberSaveable { mutableStateOf(if (entryId == -1L) 0L else entryId) }
    var isAutoSaving by remember { mutableStateOf(false) }
    
    var isVaultItem by rememberSaveable { mutableStateOf(false) }
    var unlockDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var showUnlockDatePicker by remember { mutableStateOf(false) }
    
    val contentFocusRequester = remember { FocusRequester() }

    val scope = rememberCoroutineScope()

    // ... existing entryToEdit logic ...
    val entryToEdit by if (entryId != -1L) {
        viewModel.getEntryById(entryId).collectAsStateWithLifecycle(initialValue = null)
    } else {
        remember { mutableStateOf(null) }
    }

    LaunchedEffect(entryToEdit) {
        if (entryToEdit != null && !isInitialized) {
            title = entryToEdit!!.title
            content = entryToEdit!!.content
            selectedImageUri = entryToEdit!!.imageUri?.let { uri -> android.net.Uri.parse(uri) }
            isVaultItem = entryToEdit!!.isVaultItem
            unlockDate = entryToEdit!!.unlockDate
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
    LaunchedEffect(title, content, selectedImageUri) {
        // Don't auto-save if everything is empty
        if (title.isBlank() && content.isBlank() && selectedImageUri == null) return@LaunchedEffect
        
        delay(2000) // Wait for 2 seconds of inactivity
        isAutoSaving = true
        val newId = viewModel.saveEntry(
            title = title,
            content = content,
            imageUri = selectedImageUri?.toString(),
            id = currentId,
            isDraft = true
        )
        currentId = newId
        isAutoSaving = false
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) selectedImageUri = uri }
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
                    if (isAutoSaving) {
                        Icon(
                            Icons.Default.CloudSync, 
                            contentDescription = "Saving...",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    } else if (title.isNotBlank() || content.isNotBlank() || selectedImageUri != null) {
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

            if (selectedImageUri != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    shadowElevation = 4.dp
                ) {
                    Box(modifier = Modifier.padding(8.dp)) {
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Selected image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (selectedImageUri == null) "Add Photo" else "Change Photo")
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                                imageUri = selectedImageUri?.toString(),
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
