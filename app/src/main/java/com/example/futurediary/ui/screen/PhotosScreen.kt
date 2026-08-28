package com.example.futurediary.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.futurediary.data.model.DiaryEntryWithImages
import com.example.futurediary.ui.util.journalPage
import com.example.futurediary.ui.viewmodel.DiaryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PhotosScreen(
    viewModel: DiaryViewModel,
    onNavigateBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    onEntryClick: (Long) -> Unit
) {
    val photosEntries by viewModel.photosEntries.collectAsStateWithLifecycle()
    
    val dateFormatter = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()) }
    
    // Create a list of all images with their parent entry ID
    val allImages = remember(photosEntries) {
        photosEntries.flatMap { wrap ->
            wrap.images.map { img -> wrap.entry to img }
        }
    }
    
    // Group images by formatted date string of their entries
    val groupedImages = remember(allImages) {
        allImages.groupBy { (entry, _) -> dateFormatter.format(Date(entry.date)) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Photos") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { padding ->
        if (allImages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .journalPage(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("No photos found in your diary.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .journalPage(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupedImages.forEach { (date, imagesWithEntries) ->
                    stickyHeader {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = date,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                    
                    items(imagesWithEntries.chunked(3)) { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { (entry, image) ->
                                PhotoCard(
                                    imageUri = viewModel.getImagePath(image.fileName) ?: "",
                                    onClick = { onEntryClick(entry.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Fill empty slots in the grid
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoCard(
    imageUri: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium
    ) {
        AsyncImage(
            model = imageUri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}
