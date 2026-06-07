package com.example.futurediary.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.futurediary.data.model.DiaryEntry
import com.example.futurediary.ui.viewmodel.DiaryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryListScreen(
    viewModel: DiaryViewModel,
    onNavigateToAdd: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onLogout: () -> Unit
) {
    val entries by viewModel.entries.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Future Diary") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAdd) {
                Icon(Icons.Default.Add, contentDescription = "Add Entry")
            }
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("No memories yet. Start writing!")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries) { entry ->
                    DiaryEntryItem(
                        entry = entry,
                        onClick = { onNavigateToDetail(entry.id) },
                        onDelete = { viewModel.deleteEntry(entry) }
                    )
                }
            }
        }
    }
}

@Composable
fun DiaryEntryItem(entry: DiaryEntry, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column {
            if (entry.imageUri != null) {
                AsyncImage(
                    model = entry.imageUri,
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
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.title, style = MaterialTheme.typography.titleLarge)
                        
                        val dateString = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            .format(Date(entry.date))
                        Text(
                            text = dateString,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
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
            }
        }
    }
}
