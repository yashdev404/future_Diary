package com.example.futurediary.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.futurediary.data.model.DiaryEntry
import com.example.futurediary.data.model.DiaryEntryWithImages
import com.example.futurediary.ui.util.journalPage
import com.example.futurediary.ui.viewmodel.DiaryViewModel
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    viewModel: DiaryViewModel,
    onNavigateBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    onEntryClick: (Long) -> Unit
) {
    val vaultEntries by viewModel.vaultEntries.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memory Vault") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { padding ->
        if (vaultEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .journalPage(),
                contentAlignment = Alignment.Center
            ) {
                Text("Your vault is empty. Lock a memory for the future!")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .journalPage(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(vaultEntries) { wrap ->
                    VaultEntryCard(
                        entryWithImages = wrap,
                        onClick = { onEntryClick(wrap.entry.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun VaultEntryCard(entryWithImages: DiaryEntryWithImages, onClick: () -> Unit) {
    val entry = entryWithImages.entry
    val currentTime = System.currentTimeMillis()
    val isLocked = entry.unlockDate != null && currentTime < entry.unlockDate
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { if (!isLocked) onClick() },
        enabled = !isLocked,
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = if (isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            
            if (isLocked) {
                val diff = entry.unlockDate!! - currentTime
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                Text(
                    text = "Unlocks in $days days",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Blurred content preview
                Text(
                    text = entry.content,
                    modifier = Modifier.blur(8.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = entry.content,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (entryWithImages.images.isNotEmpty()) {
                    Text(
                        text = "${entryWithImages.images.size} photos attached",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
