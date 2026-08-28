package com.example.futurediary.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.FileProvider
import com.example.futurediary.data.model.DiaryEntryWithImages
import com.example.futurediary.ui.theme.HandwritingFont
import com.example.futurediary.ui.theme.VintageLeather
import com.example.futurediary.ui.theme.VintageParchment
import com.example.futurediary.ui.viewmodel.DiaryViewModel
import com.example.futurediary.ui.viewmodel.SongMetadata
import com.example.futurediary.ui.util.RichTextUtil
import com.example.futurediary.ui.util.journalPage
import kotlinx.coroutines.delay

@Composable
fun FormattingToolbar(
    onStyleClick: (SpanStyle) -> Unit,
    onClearFormatting: () -> Unit,
    onFontToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .imePadding(),
        color = VintageLeather,
        contentColor = VintageParchment,
        shadowElevation = 8.dp
    ) {
        Column {
            // Ribbon "stitches" or top border
            HorizontalDivider(color = VintageParchment.copy(alpha = 0.3f), thickness = 1.dp)
            
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onStyleClick(SpanStyle(fontWeight = FontWeight.Bold)) }) {
                    Icon(Icons.Default.FormatBold, contentDescription = "Bold", tint = VintageParchment)
                }
                IconButton(onClick = { onStyleClick(SpanStyle(fontStyle = FontStyle.Italic)) }) {
                    Icon(Icons.Default.FormatItalic, contentDescription = "Italic", tint = VintageParchment)
                }
                IconButton(onClick = onFontToggle) {
                    Icon(Icons.Default.TextFields, contentDescription = "Font Toggle", tint = VintageParchment)
                }
                IconButton(onClick = onClearFormatting) {
                    Icon(Icons.Default.FormatClear, contentDescription = "Clear", tint = VintageParchment)
                }
                
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(VintageParchment.copy(alpha = 0.3f)))
                
                val colors = listOf(
                    VintageParchment,
                    Color(0xFFE57373), // Soft Red
                    Color(0xFF81C784), // Soft Green
                    Color(0xFF64B5F6), // Soft Blue
                    Color(0xFFFFB74D)  // Soft Orange
                )
                
                colors.forEach { color ->
                    IconButton(
                        onClick = { onStyleClick(SpanStyle(color = color)) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(1.dp, VintageParchment.copy(alpha = 0.5f), CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
    viewModel: DiaryViewModel,
    onNavigateBack: () -> Unit,
    entryId: Long = -1L,
    sharedLink: String? = null
) {
    val context = LocalContext.current
    var title by rememberSaveable { mutableStateOf("") }
    var contentValue by remember { mutableStateOf(TextFieldValue("")) }
    var selectedMood by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedImages by rememberSaveable { mutableStateOf<List<Uri>>(emptyList()) }
    var songMetadata by remember { mutableStateOf<SongMetadata?>(null) }
    var isFetchingMetadata by remember { mutableStateOf(false) }
    var isInitialized by rememberSaveable { mutableStateOf(false) }
    var currentId by rememberSaveable { mutableStateOf(if (entryId == -1L) 0L else entryId) }
    var isAutoSaving by remember { mutableStateOf(false) }
    var isSavingFinal by remember { mutableStateOf(false) }
    
    var isVaultItem by rememberSaveable { mutableStateOf(false) }
    var unlockDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var showUnlockDatePicker by remember { mutableStateOf(false) }
    
    var showPhotoSourceDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val contentFocusRequester = remember { FocusRequester() }

    val entryToEdit by if (entryId != -1L) {
        viewModel.getEntryById(entryId).collectAsStateWithLifecycle(initialValue = null)
    } else {
        remember { mutableStateOf(null) }
    }
    
    val writingPrompt by viewModel.writingPrompt.collectAsStateWithLifecycle()

    LaunchedEffect(entryToEdit) {
        if (entryToEdit != null && !isInitialized) {
            val wrap = entryToEdit!!
            title = wrap.entry.title
            contentValue = TextFieldValue(RichTextUtil.parseMarkup(wrap.entry.content))
            selectedMood = wrap.entry.mood
            selectedImages = wrap.images.map { it.fileName.toUri() } // Store filenames as URIs for the list
            isVaultItem = wrap.entry.isVaultItem
            unlockDate = wrap.entry.unlockDate
            
            if (wrap.entry.songLink != null) {
                songMetadata = SongMetadata(
                    title = wrap.entry.songTitle,
                    artist = wrap.entry.songArtist,
                    thumbnailUrl = wrap.entry.songThumbnailUrl,
                    link = wrap.entry.songLink!!
                )
            }
            
            isInitialized = true
        }
    }

    LaunchedEffect(sharedLink) {
        if (sharedLink != null && songMetadata == null && !isFetchingMetadata) {
            isFetchingMetadata = true
            val decodedLink = java.net.URLDecoder.decode(sharedLink, "UTF-8")
            val meta = viewModel.fetchSongMetadata(decodedLink)
            songMetadata = meta
            isFetchingMetadata = false
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
    LaunchedEffect(title, contentValue.annotatedString, selectedImages, selectedMood, isSavingFinal) {
        if (isSavingFinal) return@LaunchedEffect
        if (title.isBlank() && contentValue.text.isBlank() && selectedImages.isEmpty() && selectedMood == null) return@LaunchedEffect
        
        delay(2000)
        if (isSavingFinal) return@LaunchedEffect
        
        isAutoSaving = true
        val newId = viewModel.saveEntry(
            title = title,
            content = RichTextUtil.toMarkup(contentValue.annotatedString),
            images = selectedImages,
            id = currentId,
            isDraft = true,
            mood = selectedMood
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

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success && tempCameraUri != null) {
                selectedImages = (selectedImages + tempCameraUri!!).distinct()
            }
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // Launch camera logic
                val fileName = "camera_${System.currentTimeMillis()}.jpg"
                val file = java.io.File(context.filesDir, fileName)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                tempCameraUri = uri
                cameraLauncher.launch(uri)
            } else {
                android.widget.Toast.makeText(context, "Camera permission denied", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    )

    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceDialog = false },
            title = { Text("Add Photo") },
            text = { Text("Choose a source for your photo") },
            confirmButton = {
                TextButton(onClick = {
                    showPhotoSourceDialog = false
                    permissionLauncher.launch(android.Manifest.permission.CAMERA)
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Camera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Camera")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPhotoSourceDialog = false
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gallery")
                    }
                }
            }
        )
    }

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
                    IconButton(onClick = { showPhotoSourceDialog = true }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Add Photo")
                    }

                    if (isAutoSaving) {
                        Icon(
                            Icons.Default.CloudSync, 
                            contentDescription = "Saving...",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            FormattingToolbar(
                onStyleClick = { style ->
                    val selection = contentValue.selection
                    if (!selection.collapsed) {
                        val currentAnnotated = contentValue.annotatedString
                        val newAnnotatedString = buildAnnotatedString {
                            append(currentAnnotated)
                            addStyle(style, selection.start, selection.end)
                        }
                        contentValue = contentValue.copy(annotatedString = newAnnotatedString)
                    }
                },
                onClearFormatting = {
                    val selection = contentValue.selection
                    if (!selection.collapsed) {
                        contentValue = contentValue.copy(
                            annotatedString = RichTextUtil.clearStylesInRange(
                                contentValue.annotatedString,
                                selection.start,
                                selection.end
                            )
                        )
                    }
                },
                onFontToggle = {
                    val selection = contentValue.selection
                    if (!selection.collapsed) {
                        val currentStyles = contentValue.annotatedString.spanStyles.filter { 
                            it.start < selection.end && it.end > selection.start 
                        }
                        val isHandwriting = currentStyles.any { it.item.fontFamily == HandwritingFont }
                        
                        val newStyle = if (isHandwriting) {
                            SpanStyle(fontFamily = FontFamily.Default)
                        } else {
                            SpanStyle(fontFamily = HandwritingFont)
                        }
                        
                        val newAnnotatedString = buildAnnotatedString {
                            append(contentValue.annotatedString)
                            addStyle(newStyle, selection.start, selection.end)
                        }
                        contentValue = contentValue.copy(annotatedString = newAnnotatedString)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .journalPage()
                .verticalScroll(rememberScrollState())
        ) {
            // Mood Picker
            Text(
                "How are you feeling?",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            writingPrompt?.let { prompt ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val moods = listOf("😊", "😔", "😡", "😲", "😴")
                moods.forEach { mood ->
                    val isSelected = selectedMood == mood
                    Surface(
                        onClick = { selectedMood = if (isSelected) null else mood },
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(mood, style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            }

            // OutlinedTextField and rest of the content
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
                        val displayModel = if (uri.scheme == null || uri.toString().contains(context.filesDir.name)) {
                            viewModel.getImagePath(uri.toString())
                        } else {
                            uri
                        }
                        
                        Box {
                            AsyncImage(
                                model = displayModel,
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
                value = contentValue,
                onValueChange = { contentValue = it },
                label = { Text("What happened today?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 300.dp)
                    .focusRequester(contentFocusRequester)
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Soundtrack Preview
            if (songMetadata != null || isFetchingMetadata) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isFetchingMetadata) {
                            CircularProgressIndicator(modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Fetching soundtrack details...", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            songMetadata?.let { meta ->
                                if (meta.thumbnailUrl != null) {
                                    AsyncImage(
                                        model = meta.thumbnailUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = meta.title ?: "Unknown Track",
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = meta.artist ?: "Unknown Artist",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                
                                IconButton(onClick = { songMetadata = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove Soundtrack")
                                }
                            }
                        }
                    }
                }
            }

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
                    if (title.isNotBlank() && contentValue.text.isNotBlank() && !isSavingFinal) {
                        isSavingFinal = true
                        viewModel.persistEntryFinal(
                            title = title,
                            content = RichTextUtil.toMarkup(contentValue.annotatedString),
                            images = selectedImages,
                            id = currentId,
                            isVaultItem = isVaultItem,
                            unlockDate = unlockDate,
                            mood = selectedMood,
                            songLink = songMetadata?.link,
                            songTitle = songMetadata?.title,
                            songArtist = songMetadata?.artist,
                            songThumbnailUrl = songMetadata?.thumbnailUrl,
                            onComplete = onNavigateBack
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && contentValue.text.isNotBlank() && !isSavingFinal
            ) {
                if (isSavingFinal) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (entryId == -1L) "Save to Future" else "Update Memory")
                }
            }
        }
    }
}
