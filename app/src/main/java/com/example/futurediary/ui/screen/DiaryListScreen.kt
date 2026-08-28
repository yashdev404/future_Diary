package com.example.futurediary.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.futurediary.data.model.DiaryEntry
import com.example.futurediary.data.model.DiaryEntryWithImages
import com.example.futurediary.data.model.DiaryImage
import com.example.futurediary.ui.util.RichTextUtil
import com.example.futurediary.ui.viewmodel.DiaryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryListScreen(
    viewModel: DiaryViewModel,
    onNavigateToAdd: (Long?) -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAddPromise: () -> Unit,
    onOpenDrawer: () -> Unit,
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val drafts by viewModel.drafts.collectAsStateWithLifecycle()
    val flashbacks by viewModel.flashbackEntries.collectAsStateWithLifecycle()
    val comfortMemory by viewModel.comfortMemory.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(value = false) }
    val datePickerState = rememberDatePickerState()
    
    var entryToDelete by remember { mutableStateOf<DiaryEntryWithImages?>(null) }
    
    var isSearchActive by remember { mutableStateOf(value = false) }
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var showFabMenu by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setFilterDate(datePickerState.selectedDateMillis)
                        showDatePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.setFilterDate(null) // Reset filter
                        showDatePicker = false
                    },
                ) { Text("Clear Filter") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Delete Memory?") },
            text = { Text("Are you sure you want to permanently delete this memory? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        entryToDelete?.let { viewModel.deleteEntry(it) }
                        entryToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search memories...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                            )
                        )
                    } else {
                        Text("Future Diary")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { 
                            isSearchActive = !isSearchActive 
                            if (!isSearchActive) viewModel.setSearchQuery("")
                        },
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search, 
                            contentDescription = "Search",
                        )
                    }
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Filter by date")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (showFabMenu) {
                    SmallFloatingActionButton(
                        onClick = { 
                            showFabMenu = false
                            onNavigateToAddPromise() 
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Make a Promise")
                        }
                    }
                    
                    SmallFloatingActionButton(
                        onClick = { 
                            showFabMenu = false
                            onNavigateToAdd(null) 
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Write a Memory")
                        }
                    }
                }
                
                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                    containerColor = if (showFabMenu) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        if (showFabMenu) Icons.Default.Close else Icons.Default.Add, 
                        contentDescription = "Add"
                    )
                }
            }
        }
    ) { padding ->
        val outlineVariant = MaterialTheme.colorScheme.outlineVariant
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (entries.isNotEmpty() || drafts.isNotEmpty()) {
                // Timeline thread line
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val lineX = 16.dp.toPx() + 30.dp.toPx() // LazyColumn padding + half sidebar width
                    drawLine(
                        color = outlineVariant,
                        start = Offset(lineX, 0f),
                        end = Offset(lineX, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            if (entries.isEmpty() && drafts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No memories yet. Start writing!")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Intelligent Companion Card (Trend-based)
                    comfortMemory?.let { memory ->
                        item {
                            CompanionMessageCard(
                                memory = memory,
                                onClick = { onNavigateToDetail(memory.entry.id) }
                            )
                        }
                    }

                    if (flashbacks.isNotEmpty()) {
                        item {
                            FlashbackCard(
                                flashback = flashbacks.first(), // Show the most recent flashback
                                onClick = { onNavigateToDetail(flashbacks.first().entry.id) }
                            )
                        }
                    }

                    if (drafts.isNotEmpty()) {
                        item {
                            Text(
                                "Unfinished Memories",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 68.dp, bottom = 8.dp)
                            )
                        }
                        items(drafts) { draftItem ->
                            DiaryEntryItem(
                                entryWithImages = draftItem,
                                onClick = { onNavigateToAdd(draftItem.entry.id) },
                                getImagePath = { viewModel.getImagePath(it) },
                                onDelete = { entryToDelete = draftItem }
                            )
                        }
                        if (entries.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HorizontalDivider(
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        thickness = 1.dp
                                    )
                                    Text(
                                        text = "Completed Memories",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        thickness = 1.dp
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }

                    items(entries) { entryItem ->
                        DiaryEntryItem(
                            entryWithImages = entryItem,
                            onClick = { onNavigateToDetail(entryItem.entry.id) },
                            getImagePath = { viewModel.getImagePath(it) },
                            onDelete = { entryToDelete = entryItem }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompanionMessageCard(
    memory: DiaryEntryWithImages,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "A gentle light for a heavy day...",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "I've noticed things have been tough lately. Take a moment to breathe and remember this happy day:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (memory.images.isNotEmpty()) {
                    AsyncImage(
                        model = memory.images.first().fileName,
                        contentDescription = null,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                
                Column {
                    Text(
                        text = memory.entry.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = RichTextUtil.stripFormatting(memory.entry.content),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun FlashbackCard(
    flashback: DiaryEntryWithImages,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                val yearsAgo = remember(flashback.entry.date) {
                    val entryCal = java.util.Calendar.getInstance().apply { timeInMillis = flashback.entry.date }
                    java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - entryCal.get(java.util.Calendar.YEAR)
                }
                Text(
                    text = "A memory from $yearsAgo ${if (yearsAgo == 1) "year" else "years"} ago",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (flashback.images.isNotEmpty()) {
                    AsyncImage(
                        model = flashback.images.first().fileName,
                        contentDescription = null,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                
                Column {
                    Text(
                        text = flashback.entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = RichTextUtil.stripFormatting(flashback.entry.content),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun DiaryEntryItem(
    entryWithImages: DiaryEntryWithImages, 
    onClick: () -> Unit, 
    getImagePath: (String?) -> String?,
    onDelete: () -> Unit
) {
    val entry = entryWithImages.entry
    val images = entryWithImages.images
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Date Sidebar (Left)
        DateSidebar(date = entry.date)
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Content Card (Right)
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable { onClick() },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column {
                // Hero Image
                if (images.isNotEmpty()) {
                    AsyncImage(
                        model = getImagePath(images.first().fileName),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                
                Column(modifier = Modifier.padding(12.dp)) {
                    // Metadata Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
                            Text(
                                text = timeFormat.format(Date(entry.date)),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            if (entry.mood != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(entry.mood, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Body
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = RichTextUtil.stripFormatting(entry.content),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Thumbnail Gallery
                    if (images.size > 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                        ThumbnailGallery(
                            images = images.drop(1),
                            getImagePath = getImagePath
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateSidebar(date: Long, modifier: Modifier = Modifier) {
    val dayFormat = remember { SimpleDateFormat("dd", Locale.getDefault()) }
    val monthFormat = remember { SimpleDateFormat("MMM", Locale.getDefault()) }
    val dateObj = Date(date)
    
    Box(
        modifier = modifier.width(60.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 4.dp,
            modifier = Modifier.size(54.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = dayFormat.format(dateObj),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = monthFormat.format(dateObj).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun ThumbnailGallery(
    images: List<DiaryImage>,
    getImagePath: (String?) -> String?,
    modifier: Modifier = Modifier
) {
    val maxVisible = 3
    val displayImages = images.take(maxVisible)
    val remainingCount = images.size - maxVisible

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        displayImages.forEachIndexed { index, image ->
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small)
            ) {
                AsyncImage(
                    model = getImagePath(image.fileName),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                if (index == maxVisible - 1 && remainingCount > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+$remainingCount",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
