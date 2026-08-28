package com.example.futurediary.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.Brush
import android.net.Uri
import android.content.Intent
import androidx.compose.ui.draw.shadow
import com.example.futurediary.ui.theme.VintageInk
import com.example.futurediary.ui.theme.VintageParchment
import com.example.futurediary.ui.util.journalPage
import com.example.futurediary.ui.viewmodel.AuthViewModel
import com.example.futurediary.ui.viewmodel.DiaryViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalContext
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: DiaryViewModel,
    authViewModel: AuthViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val soulSummary by viewModel.soulSummary.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isAnonymous by authViewModel.isAnonymous.collectAsStateWithLifecycle()
    
    var isMonthlyExpanded by remember { mutableStateOf(true) }
    var isSongExpanded by remember { mutableStateOf(true) }
    
    var showNameDialog by remember { mutableStateOf(false) }
    var showImageDialog by remember { mutableStateOf(false) }
    var showLinkAccountDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> 
            if (uri != null) {
                viewModel.updateProfile(userProfile.name, uri)
            }
        }
    )

    if (showNameDialog) {
        var newName by remember { mutableStateOf(userProfile.name) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Edit Name") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Your Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateProfile(newName, null)
                    showNameDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showImageDialog) {
        Dialog(onDismissRequest = { showImageDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (userProfile.profileImageFileName != null) {
                        SubcomposeAsyncImage(
                            model = viewModel.getImagePath(userProfile.profileImageFileName),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                            loading = {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            },
                            error = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.errorContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Error, contentDescription = null)
                                }
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(120.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change Photo")
                    }
                    TextButton(
                        onClick = { showImageDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }

    if (showLinkAccountDialog) {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        var isLinking by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isLinking) showLinkAccountDialog = false },
            title = { Text("Link Account for Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Secure your memories by creating a permanent account.")
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLinking
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLinking
                    )
                    if (error != null) {
                        Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (email.isNotBlank() && password.isNotBlank()) {
                            isLinking = true
                            authViewModel.linkAccount(email, password) { success, msg ->
                                isLinking = false
                                if (success) {
                                    showLinkAccountDialog = false
                                } else {
                                    error = msg
                                }
                            }
                        }
                    },
                    enabled = !isLinking
                ) {
                    if (isLinking) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Link Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkAccountDialog = false }, enabled = !isLinking) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & Insights") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .journalPage(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. User Header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { showImageDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (userProfile.profileImageFileName != null) {
                            SubcomposeAsyncImage(
                                model = viewModel.getImagePath(userProfile.profileImageFileName),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                loading = { CircularProgressIndicator(modifier = Modifier.padding(16.dp)) }
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showNameDialog = true }
                    ) {
                        Text(
                            text = userProfile.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Name",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 1b. Cloud Sync & Backup Status
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isAnonymous) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f) 
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, 
                        if (isAnonymous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isAnonymous) Icons.Default.CloudOff else Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = if (isAnonymous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                if (isAnonymous) "Backup Disabled" else "Cloud Sync Active",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (isAnonymous) "Your memories are only on this device. Create a permanent account to back them up to the cloud."
                            else "Your journey is safely backed up to the cloud and synced across your devices.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (isAnonymous) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showLinkAccountDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Secure My Memories")
                            }
                        } else {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    val syncRequest = OneTimeWorkRequestBuilder<com.example.futurediary.data.sync.SyncWorker>().build()
                                    WorkManager.getInstance(context).enqueue(syncRequest)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sync Now")
                            }
                        }
                    }
                }
            }

            // 2. Mood Insights
            item {
                InsightCard(title = "Emotional Insights (Last 90 Days)") {
                    if (stats.moodDistribution.isEmpty()) {
                        Text("No mood data yet. Start tracking your feelings!", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            stats.moodDistribution.forEach { (emoji, count) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(emoji, style = MaterialTheme.typography.headlineMedium)
                                    Text(
                                        text = count.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "times",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // 2b. Spotify/YouTube Rewind Style Cards
            soulSummary?.let { summary ->
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp), // More space from sides
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Card 1: Your Soul Portrait
                        RewindCard(
                            title = "Monthly Soul Portrait",
                            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
                            isExpanded = isMonthlyExpanded,
                            onToggle = { isMonthlyExpanded = !isMonthlyExpanded },
                            icon = {
                                Text(
                                    text = summary.dominantMood ?: "✨",
                                    style = MaterialTheme.typography.displaySmall, // Shrunk icon
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        ) {
                            Text(
                                text = summary.summaryText ?: "",
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        // Card 2: Soundtrack of the Month
                        val hasSong = summary.suggestedSongMetadata != null
                        RewindCard(
                            title = "Soundtrack of your Month",
                            colors = if (hasSong) listOf(Color(0xFF64B5F6), Color(0xFF1E88E5)) else listOf(Color.Gray, Color.DarkGray), // Wavy Blue
                            isExpanded = isSongExpanded,
                            onToggle = { isSongExpanded = !isSongExpanded },
                            icon = {
                                if (hasSong) {
                                    AsyncImage(
                                        model = summary.suggestedSongMetadata?.thumbnailUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(70.dp) // Shrunk size
                                            .clip(RoundedCornerShape(12.dp))
                                            .shadow(8.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.MusicOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        ) {
                            if (hasSong) {
                                Column {
                                    Text(
                                        text = summary.suggestedSongMetadata?.title ?: "Unknown Track",
                                        style = MaterialTheme.typography.titleSmall, // Smaller font
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = summary.suggestedSongMetadata?.artist ?: "Unknown Artist",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(summary.suggestedSongMetadata?.link))
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                        shape = RoundedCornerShape(24.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Replay", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            } else {
                                Text(
                                    text = "No soundtrack yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Memory Streak
            item {
                InsightCard(title = "Memory Streak") {
                    ContributionGrid(activityMap = stats.activityMap)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StreakStat(label = "Current Streak", value = "${stats.currentStreak} Days", icon = Icons.Default.Whatshot)
                        StreakStat(label = "Longest Streak", value = "${stats.longestStreak} Days", icon = Icons.Default.EmojiEvents)
                    }
                }
            }

            // 4. Overall Stats
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Total Memories",
                        value = stats.totalEntries.toString(),
                        icon = Icons.AutoMirrored.Filled.MenuBook
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Words Written",
                        value = stats.totalWords.toString(),
                        icon = Icons.Default.Create
                    )
                }
            }

            // 5. Top Words Analysis
            item {
                InsightCard(title = "Top Emotional Words") {
                    if (stats.topWords.isEmpty()) {
                        Text("Write more memories to see analysis!", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        stats.topWords.forEach { (word, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = word.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Box(
                                    modifier = Modifier
                                        .height(8.dp)
                                        .weight(1f)
                                        .padding(horizontal = 16.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    val progress = count.toFloat() / (stats.topWords.firstOrNull()?.second ?: 1)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(progress)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RewindCard(
    title: String,
    colors: List<Color>,
    isExpanded: Boolean = true,
    onToggle: () -> Unit = {},
    icon: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val cornerRadius = if (isExpanded) 24.dp else 40.dp
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(cornerRadius),
        shadowElevation = if (isExpanded) 8.dp else 2.dp
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(colors))
                .padding(if (isExpanded) 20.dp else 12.dp)
        ) {
            if (isExpanded) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = "Collapse",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(modifier = Modifier.sizeIn(maxWidth = 80.dp, maxHeight = 80.dp)) {
                            icon()
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            content()
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InsightCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun StreakStat(label: String, value: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun ContributionGrid(activityMap: Map<Long, Int>) {
    // Show last 15 weeks (roughly 3 months) to fit on screen nicely
    val weeksToShow = 15
    val dayWidth = 18.dp // Increased size
    val spacing = 4.dp
    
    var selectedDay by remember { mutableStateOf<Pair<Long, Int>?>(null) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Tooltip Bubble
        Box(
            modifier = Modifier
                .height(30.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (selectedDay != null) {
                val (timestamp, count) = selectedDay!!
                val sdf = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 4.dp
                ) {
                    Text(
                        text = "${sdf.format(Date(timestamp))}: $count ${if (count == 1) "memory" else "memories"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Month Labels Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            val monthSdf = SimpleDateFormat("MMM", Locale.getDefault())
            var lastMonth = ""
            
            for (w in 0 until weeksToShow) {
                val weekOffset = (weeksToShow - 1 - w) * 7
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -weekOffset)
                }
                val currentMonth = monthSdf.format(cal.time)
                
                Box(modifier = Modifier.width(dayWidth + spacing)) {
                    if (currentMonth != lastMonth) {
                        Text(
                            text = currentMonth,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.offset(y = (-12).dp)
                        )
                        lastMonth = currentMonth
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            for (w in 0 until weeksToShow) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                    for (d in 0 until 7) {
                        val dayOffset = (weeksToShow - 1 - w) * 7 + (6 - d)
                        val checkCalendar = Calendar.getInstance()
                        checkCalendar.add(Calendar.DAY_OF_YEAR, -dayOffset)
                        
                        // Normalize to start of day
                        checkCalendar.set(Calendar.HOUR_OF_DAY, 0)
                        checkCalendar.set(Calendar.MINUTE, 0)
                        checkCalendar.set(Calendar.SECOND, 0)
                        checkCalendar.set(Calendar.MILLISECOND, 0)
                        
                        val timestamp = checkCalendar.timeInMillis
                        val count = activityMap[timestamp] ?: 0
                        
                        val color = when {
                            count >= 3 -> MaterialTheme.colorScheme.primary
                            count == 2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            count == 1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        }

                        Box(
                            modifier = Modifier
                                .size(dayWidth)
                                .clip(RoundedCornerShape(3.dp)) // Slightly more rounded for larger size
                                .background(color)
                                .clickable { 
                                    selectedDay = if (selectedDay?.first == timestamp) null else timestamp to count
                                }
                        )
                    }
                }
                Spacer(modifier = Modifier.width(spacing))
            }
        }
    }
}
