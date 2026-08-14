package com.example.futurediary.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val entry by viewModel.getEntryById(entryId).collectAsStateWithLifecycle(initialValue = null)
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

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
                    .padding(16.dp) // Outer padding for the "page"
            ) {
                // 1. Header (Title & Date) - Outside the lines
                Text(
                    text = diaryEntry.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                val dateFormatter = remember { SimpleDateFormat("MMMM dd, yyyy - HH:mm", Locale.getDefault()) }
                val date = dateFormatter.format(Date(diaryEntry.date))
                
                Text(
                    text = date,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                // 2. Framed Image
                if (diaryEntry.imageUri != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        shadowElevation = 4.dp
                    ) {
                        Box(modifier = Modifier.padding(8.dp)) { // The "Polaroid" border
                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                AsyncImage(
                                    model = diaryEntry.imageUri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
                
                // 3. Lined Paper Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = screenHeight)
                        .drawNotebookLines(lineSpacing = 32.dp) // Matches Typography line height
                        .padding(start = 48.dp, top = 0.dp, end = 16.dp, bottom = 32.dp)
                ) {
                    // Small spacer to align the first line of text with the first notebook line
                    // Since line height is 32.sp (almost 32.dp), we push it down slightly
                    Spacer(modifier = Modifier.height(10.dp)) 
                    
                    Text(
                        text = diaryEntry.content,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 32.sp // Ensure exact matching
                    )
                    
                    Spacer(modifier = Modifier.height(100.dp))
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
