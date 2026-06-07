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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.futurediary.ui.viewmodel.DiaryViewModel

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

    // Fetch data if editing
    val entryToEdit by if (entryId != -1L) {
        viewModel.getEntryById(entryId).collectAsState(initial = null)
    } else {
        remember { mutableStateOf<com.example.futurediary.data.model.DiaryEntry?>(null) }
    }

    LaunchedEffect(entryToEdit) {
        if (entryToEdit != null && !isInitialized) {
            title = entryToEdit!!.title
            content = entryToEdit!!.content
            selectedImageUri = entryToEdit!!.imageUri?.let { uri -> android.net.Uri.parse(uri) }
            isInitialized = true
        }
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
            // ... (rest of the fields remain same)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (selectedImageUri != null) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "Selected image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(bottom = 16.dp),
                    contentScale = ContentScale.Crop
                )
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
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        viewModel.saveEntry(
                            title = title,
                            content = content,
                            imageUri = selectedImageUri?.toString(),
                            id = if (entryId == -1L) 0L else entryId
                        )
                        onNavigateBack()
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
