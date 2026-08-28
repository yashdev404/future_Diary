package com.example.futurediary.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.futurediary.data.model.Promise
import com.example.futurediary.ui.util.journalPage
import com.example.futurediary.ui.viewmodel.DiaryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromisesScreen(
    viewModel: DiaryViewModel,
    onOpenDrawer: () -> Unit
) {
    val allPromises by viewModel.promises.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf("All") }
    
    val filteredPromises = when (selectedFilter) {
        "Kept" -> allPromises.filter { it.isCompleted }
        "On-going" -> allPromises.filter { !it.isCompleted }
        else -> allPromises
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Promises") },
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
        ) {
            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "On-going", "Kept").forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }

            if (filteredPromises.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().journalPage(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No promises found in this category.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().journalPage(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredPromises) { promise ->
                        PromiseItem(
                            promise = promise,
                            onToggle = { viewModel.updatePromiseStatus(promise, !promise.isCompleted) },
                            onDelete = { viewModel.deletePromise(promise) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PromiseItem(
    promise: Promise,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (promise.isCompleted) 
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f) 
                else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = promise.isCompleted,
                onCheckedChange = { onToggle() }
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = promise.content,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (promise.isCompleted) FontWeight.Normal else FontWeight.Medium,
                    textDecoration = if (promise.isCompleted) TextDecoration.LineThrough else null,
                    color = if (promise.isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Made on: ${sdf.format(Date(promise.date))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete, 
                    contentDescription = "Delete", 
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
