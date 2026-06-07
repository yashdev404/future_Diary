package com.example.futurediary.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.futurediary.ui.util.drawNotebookLines
import com.example.futurediary.ui.viewmodel.DiaryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryDetailScreen(
    entryId: Long,
    viewModel: DiaryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit
) {
    val entry by viewModel.getEntryById(entryId).collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memory Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToEdit(entryId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            )
        }
    ) { padding ->
        entry?.let { diaryEntry ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                if (diaryEntry.imageUri != null) {
                    AsyncImage(
                        model = diaryEntry.imageUri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .drawNotebookLines()
                        .padding(start = 48.dp, top = 32.dp, end = 16.dp, bottom = 32.dp)
                ) {
                    Text(
                        text = diaryEntry.title,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    
                    val date = SimpleDateFormat("MMMM dd, yyyy - HH:mm", Locale.getDefault())
                        .format(Date(diaryEntry.date))
                    
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = diaryEntry.content,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } ?: Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
