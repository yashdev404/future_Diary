package com.example.futurediary.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.futurediary.data.model.DiaryEntry
import com.example.futurediary.data.model.DiaryEntryWithImages
import com.example.futurediary.ui.util.journalPage
import com.example.futurediary.ui.viewmodel.DiaryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryListScreen(
    viewModel: DiaryViewModel,
    onNavigateToAdd: (Long?) -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onOpenDrawer: () -> Unit,
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val drafts by viewModel.drafts.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(value = false) }
    val datePickerState = rememberDatePickerState()
    
    var entryToDelete by remember { mutableStateOf<DiaryEntryWithImages?>(null) }
    
    var isSearchActive by remember { mutableStateOf(value = false) }
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

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
            FloatingActionButton(onClick = { onNavigateToAdd(null) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Entry")
            }
        }
    ) { padding ->
        if (entries.isEmpty() && drafts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .journalPage(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("No memories yet. Start writing!")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .journalPage(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (drafts.isNotEmpty()) {
                    item {
                        Text(
                            "Unfinished Memories",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(drafts) { draftItem ->
                        DiaryEntryItem(
                            entryWithImages = draftItem,
                            onClick = { onNavigateToAdd(draftItem.entry.id) }
                        ) { entryToDelete = draftItem }
                    }
                    if (entries.isNotEmpty()) {
                        item {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }

                items(entries) { entryItem ->
                    DiaryEntryItem(
                        entryWithImages = entryItem,
                        onClick = { onNavigateToDetail(entryItem.entry.id) }
                    ) { entryToDelete = entryItem }
                }
            }
        }
    }
}

@Composable
fun DiaryEntryItem(
    entryWithImages: DiaryEntryWithImages, 
    onClick: () -> Unit, 
    onDelete: () -> Unit
) {
    val entry = entryWithImages.entry
    val images = entryWithImages.images
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column {
            if (images.isNotEmpty()) {
                AsyncImage(
                    model = images.first().imageUri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.title, style = MaterialTheme.typography.titleLarge)
                        
                        val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
                        val dateString = dateFormatter.format(Date(entry.date))
                        Text(
                            text = dateString,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = entry.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                if (images.size > 1) {
                    Text(
                        text = "+${images.size - 1} more photos",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
