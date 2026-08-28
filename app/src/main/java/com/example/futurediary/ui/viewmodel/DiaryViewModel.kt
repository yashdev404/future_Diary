package com.example.futurediary.ui.viewmodel

import android.util.Log
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.futurediary.data.model.DiaryEntry
import com.example.futurediary.data.model.DiaryEntryWithImages
import com.example.futurediary.data.model.DiaryImage
import com.example.futurediary.data.model.ProfileStats
import com.example.futurediary.data.model.Promise
import com.example.futurediary.data.model.UserProfile
import com.example.futurediary.data.repository.DiaryRepository
import com.example.futurediary.ui.util.RichTextUtil
import com.example.futurediary.ui.util.STOP_WORDS
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import java.util.*
import javax.inject.Inject

private const val TAG = "DiaryViewModel"

data class SongMetadata(
    val title: String? = null,
    val artist: String? = null,
    val thumbnailUrl: String? = null,
    val link: String
)

data class SoulSummary(
    val dominantMood: String? = null,
    val dominantGenre: String? = null,
    val suggestedSongMetadata: SongMetadata? = null,
    val summaryText: String? = null
)

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val repository: DiaryRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val _currentUserId = MutableStateFlow(auth.currentUser?.uid)
    private val _filterDate = MutableStateFlow<Long?>(null) // null means show all
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    @OptIn(ExperimentalCoroutinesApi::class)
    val userProfile: StateFlow<UserProfile> = _currentUserId.flatMapLatest { userId ->
        if (userId != null) {
            repository.getUserProfile(userId).map { it ?: UserProfile(userId) }
        } else {
            flowOf(UserProfile(""))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserProfile("")
    )

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val entries: StateFlow<List<DiaryEntryWithImages>> = combine(
        _currentUserId,
        _filterDate,
        _searchQuery
    ) { userId, filterDate, query ->
        Triple(userId, filterDate, query)
    }.debounce { (_, _, query) ->
        if (query.isEmpty()) 0L else 300L
    }.flatMapLatest { (userId, filterDate, query) ->
        if (userId != null) {
            val baseFlow = if (filterDate == null) {
                repository.getAllEntries(userId)
            } else {
                val startOfDay = getStartOfDay(filterDate)
                val endOfDay = startOfDay + 86400000L
                repository.getEntriesByDateRange(userId, startOfDay, endOfDay)
            }
            
            baseFlow.map { entriesList ->
                if (query.isBlank()) {
                    entriesList
                } else {
                    // Smart Conceptual Search
                    val relatedKeywords = when (query.lowercase()) {
                        "nature" -> listOf("nature", "tree", "park", "sun", "rain", "beach", "sky", "flower")
                        "work" -> listOf("work", "project", "office", "meeting", "task", "code", "sky pulse")
                        "sad" -> listOf("sad", "unhappy", "gloomy", "heavy", "tough", "down")
                        "happy" -> listOf("happy", "smile", "joy", "great", "wonderful", "celebrate")
                        else -> listOf(query.lowercase())
                    }
                    
                    entriesList.filter { item ->
                        val plainContent = RichTextUtil.stripFormatting(item.entry.content).lowercase()
                        val title = item.entry.title.lowercase()
                        relatedKeywords.any { kw -> title.contains(kw) || plainContent.contains(kw) }
                    }
                }
            }
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val flashbackEntries: StateFlow<List<DiaryEntryWithImages>> = entries.map { allEntries ->
        val calendar = Calendar.getInstance()
        val todayDay = calendar.get(Calendar.DAY_OF_MONTH)
        val todayMonth = calendar.get(Calendar.MONTH)
        val todayYear = calendar.get(Calendar.YEAR)
        
        allEntries.filter { item ->
            val entryCal = Calendar.getInstance()
            entryCal.timeInMillis = item.entry.date
            val entryDay = entryCal.get(Calendar.DAY_OF_MONTH)
            val entryMonth = entryCal.get(Calendar.MONTH)
            val entryYear = entryCal.get(Calendar.YEAR)
            
            // It's the same day and month, but a different year
            entryDay == todayDay && entryMonth == todayMonth && entryYear < todayYear
        }.sortedByDescending { it.entry.date }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val comfortMemory: StateFlow<DiaryEntryWithImages?> = entries.map { allEntries ->
        val todayStart = getStartOfDay(System.currentTimeMillis())
        
        // 1. Check for 5+ sad entries today
        val todaySadEntries = allEntries.filter { 
            getStartOfDay(it.entry.date) == todayStart && (it.entry.mood == "😔" || it.entry.mood == "😡")
        }
        
        // 2. Check for 3+ consecutive days of sadness
        var consecutiveSadDays = 0
        for (i in 0 until 5) { // Check last 5 days
            val checkDay = todayStart - (i * 86400000L)
            val dayHasSadness = allEntries.any { 
                getStartOfDay(it.entry.date) == checkDay && (it.entry.mood == "😔" || it.entry.mood == "😡")
            }
            if (dayHasSadness) consecutiveSadDays++ else if (i < 3) break // Break if gap in first 3 days
        }

        if (todaySadEntries.size >= 5 || consecutiveSadDays >= 3) {
            val happyEntries = allEntries.filter { it.entry.mood == "😊" }
            if (happyEntries.isNotEmpty()) happyEntries.random() else null
        } else {
            null
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val writingPrompt: StateFlow<String?> = entries.map { allEntries ->
        val todayStart = getStartOfDay(System.currentTimeMillis())
        var consecutiveHappyDays = 0
        for (i in 0 until 3) {
            val checkDay = todayStart - (i * 86400000L)
            if (allEntries.any { getStartOfDay(it.entry.date) == checkDay && it.entry.mood == "😊" }) {
                consecutiveHappyDays++
            }
        }
        
        when {
            consecutiveHappyDays >= 3 -> "You've been in a great mood lately! What's one small thing that made you smile today?"
            allEntries.isEmpty() -> "Welcome to your future diary. What's one thing you want to remember about today?"
            else -> null
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )


    @OptIn(ExperimentalCoroutinesApi::class)
    val drafts: StateFlow<List<DiaryEntryWithImages>> = _currentUserId
        .flatMapLatest { userId ->
            if (userId != null) {
                repository.getAllDrafts(userId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val vaultEntries: StateFlow<List<DiaryEntryWithImages>> = _currentUserId
        .flatMapLatest { userId ->
            if (userId != null) {
                repository.getVaultEntries(userId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val photosEntries: StateFlow<List<DiaryEntryWithImages>> = combine(
        entries,
        vaultEntries
    ) { entriesList, vaultList ->
        (entriesList + vaultList).filter { it.images.isNotEmpty() }
            .sortedByDescending { it.entry.date }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val promises: StateFlow<List<Promise>> = _currentUserId
        .flatMapLatest { userId ->
            if (userId != null) {
                repository.getAllPromises(userId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val stats: StateFlow<ProfileStats> = combine(
        entries,
        vaultEntries
    ) { mainList, vaultList ->
        val allEntriesWithImages = mainList + vaultList
        if (allEntriesWithImages.isEmpty()) return@combine ProfileStats()

        val startTime = System.currentTimeMillis()

        // 1. Word Analysis
        val wordCounts = allEntriesWithImages.asSequence()
            .map { RichTextUtil.stripFormatting(it.entry.content) }
            .flatMap { it.split(Regex("\\W+")).asSequence() }
            .map { it.lowercase() }
            .filter { it.length > 2 && it !in STOP_WORDS }
            .groupingBy { it }
            .eachCount()

        val topWords = wordCounts.toList()
            .sortedByDescending { it.second }
            .take(5)

        val totalWords = allEntriesWithImages.sumOf { 
            RichTextUtil.stripFormatting(it.entry.content).split(Regex("\\W+")).size 
        }

        // 2. Activity & Streak Map
        val activityMap = allEntriesWithImages.groupBy { getStartOfDay(it.entry.date) }
            .mapValues { it.value.size }

        // 3. Streak Calculation
        val sortedDates = activityMap.keys.sortedDescending()
        var currentStreak = 0
        var longestStreak = 0
        var tempStreak = 0
        
        val today = getStartOfDay(System.currentTimeMillis())
        val yesterday = today - 86400000L

        // Current Streak
        if (activityMap.containsKey(today) || activityMap.containsKey(yesterday)) {
            var checkDate = if (activityMap.containsKey(today)) today else yesterday
            while (activityMap.containsKey(checkDate)) {
                currentStreak++
                checkDate -= 86400000L
            }
        }

        // Longest Streak
        val allDatesAsc = activityMap.keys.sorted()
        if (allDatesAsc.isNotEmpty()) {
            var lastDate = allDatesAsc.first()
            tempStreak = 1
            longestStreak = 1
            for (i in 1 until allDatesAsc.size) {
                if (allDatesAsc.elementAt(i) == lastDate + 86400000L) {
                    tempStreak++
                } else {
                    tempStreak = 1
                }
                lastDate = allDatesAsc.elementAt(i)
                if (tempStreak > longestStreak) longestStreak = tempStreak
            }
        }

        Log.d(TAG, "Stats calculated in ${System.currentTimeMillis() - startTime}ms")

        ProfileStats(
            totalEntries = allEntriesWithImages.size,
            totalWords = totalWords,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            topWords = topWords,
            activityMap = activityMap,
            moodDistribution = allEntriesWithImages.asSequence()
                .filter { it.entry.date > System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000) }
                .mapNotNull { it.entry.mood }
                .groupingBy { it }
                .eachCount()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileStats()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val soulSummary: StateFlow<SoulSummary?> = combine(stats, entries) { currentStats, allEntries ->
        if (currentStats.totalEntries < 3) return@combine null
        
        val topMood = currentStats.moodDistribution.maxByOrNull { it.value }?.key
        val topWord = currentStats.topWords.firstOrNull()?.first ?: "your journey"
        
        // Find a song from history that matches mood, or the most recent song
        val recentEntriesWithSongs = allEntries.filter { it.entry.songLink != null }
            .filter { it.entry.date > System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000) }
        
        val bestSongEntry = recentEntriesWithSongs.find { it.entry.mood == topMood } 
            ?: recentEntriesWithSongs.firstOrNull()

        val suggestedMetadata = bestSongEntry?.let {
            SongMetadata(
                title = it.entry.songTitle,
                artist = it.entry.songArtist,
                thumbnailUrl = it.entry.songThumbnailUrl,
                link = it.entry.songLink!!
            )
        }

        val genre = when (topMood) {
            "😊" -> "Upbeat & Joyful"
            "😔" -> "Melancholic Lo-fi"
            "😡" -> "Intense & Powerful"
            "😲" -> "Experimental & New"
            else -> "Calm & Reflective"
        }
        
        SoulSummary(
            dominantMood = topMood,
            dominantGenre = genre,
            suggestedSongMetadata = suggestedMetadata,
            summaryText = "This month has been a journey of $topWord. You've mostly felt $topMood, showing a real focus on what matters to you."
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )


    fun setFilterDate(timestamp: Long?) {
        _filterDate.value = timestamp
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Resolves a filename or path into a full file path for display.
     * Defensive: Returns absolute paths directly if detected.
     */
    fun getImagePath(fileName: String?): String? {
        if (fileName.isNullOrBlank()) return null
        if (fileName.startsWith("/") || fileName.startsWith("file://")) return fileName
        return File(context.filesDir, fileName).absolutePath
    }

    fun getEntryById(id: Long) : Flow<DiaryEntryWithImages?> {
        val userId = auth.currentUser?.uid ?: return flowOf(null)
        return repository.getEntryById(id, userId)
    }

    fun updateProfile(name: String, imageUri: Uri?) {
        val userId = auth.currentUser?.uid ?: return
        val finalName = if (name.isBlank()) "Journal Owner" else name
        
        viewModelScope.launch {
            Log.d(TAG, "Updating profile for user: $userId, name: $finalName")
            var permanentImageFileName: String? = userProfile.value.profileImageFileName
            
            if (imageUri != null) {
                // If it's a new URI (from picker), save it
                if (!imageUri.toString().contains(context.filesDir.name)) {
                    Log.d(TAG, "New profile image detected: $imageUri")
                    permanentImageFileName = saveImageToInternalStorage(imageUri)
                }
            }

            repository.upsertUserProfile(
                UserProfile(
                    userId = userId,
                    name = finalName,
                    profileImageFileName = permanentImageFileName
                )
            )
            Log.d(TAG, "Profile upserted successfully")
        }
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun updateCurrentUser() {
        _currentUserId.value = auth.currentUser?.uid
    }

    suspend fun fetchSongMetadata(url: String): SongMetadata? = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .get()
            
            val ogTitle = doc.select("meta[property=og:title]").attr("content")
            val ogDescription = doc.select("meta[property=og:description]").attr("content")
            val ogImage = doc.select("meta[property=og:image]").attr("content")
            
            // Heuristic to split title and artist if title contains both
            var title = ogTitle
            var artist = ogDescription
            
            if (ogTitle.contains(" - ")) {
                val parts = ogTitle.split(" - ", limit = 2)
                title = parts[0]
                artist = parts[1]
            } else if (ogTitle.contains(" by ")) {
                val parts = ogTitle.split(" by ", limit = 2)
                title = parts[0]
                artist = parts[1]
            }

            SongMetadata(
                title = title.takeIf { it.isNotBlank() },
                artist = artist.takeIf { it.isNotBlank() },
                thumbnailUrl = ogImage.takeIf { it.isNotBlank() },
                link = url
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch metadata for $url", e)
            null
        }
    }

    suspend fun saveEntry(
        title: String,
        content: String,
        images: List<Uri>,
        id: Long = 0,
        isDraft: Boolean = false,
        isVaultItem: Boolean = false,
        unlockDate: Long? = null,
        mood: String? = null,
        songLink: String? = null,
        songTitle: String? = null,
        songArtist: String? = null,
        songThumbnailUrl: String? = null
    ): Long {
        val userId = auth.currentUser?.uid ?: return 0
        
        val diaryEntry = DiaryEntry(
            id = id,
            userId = userId,
            date = System.currentTimeMillis(),
            title = title,
            content = content,
            imageUri = null, // Deprecated
            isDraft = isDraft,
            isVaultItem = isVaultItem,
            unlockDate = unlockDate,
            mood = mood,
            songLink = songLink,
            songTitle = songTitle,
            songArtist = songArtist,
            songThumbnailUrl = songThumbnailUrl,
            lastModified = System.currentTimeMillis(),
            isSynced = false
        )
        
        val entryId = repository.insertEntry(diaryEntry)
        
        // Handle images: process new ones, keep existing filenames
        val fileNames = images.mapNotNull { uri ->
            val uriString = uri.toString()
            if (uriString.contains(context.filesDir.name)) {
                // It's already an internal file (just the filename or relative path)
                uriString.substringAfterLast("/")
            } else {
                // It's a new external URI, validate and save it
                saveImageToInternalStorage(uri)
            }
        }
        
        // Update database relationship: delete old, insert new
        repository.deleteImagesForEntry(entryId)
        
        val diaryImages = fileNames.map { fileName ->
            DiaryImage(
                entryId = entryId, 
                fileName = fileName,
                mimeType = context.contentResolver.getType(fileName.toUri()) ?: "image/jpeg"
            )
        }
        
        if (diaryImages.isNotEmpty()) {
            repository.insertImages(diaryImages)
        }
        
        Log.d(TAG, "Entry saved with id $entryId (images: ${diaryImages.size})")
        return entryId
    }

    private suspend fun saveImageToInternalStorage(uri: Uri): String? = withContext(Dispatchers.IO) {
        // 1. Validation: Type
        val type = context.contentResolver.getType(uri) ?: "image/jpeg"
        if (!type.startsWith("image/")) {
            Log.e(TAG, "Rejected: Not an image ($type)")
            return@withContext null
        }

        // 2. Validation: Size (20MB Limit)
        val size = try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0
        } catch (e: Exception) { 0 }
        
        if (size > 20 * 1024 * 1024) {
            Log.e(TAG, "Rejected: File too large (${size / 1024 / 1024}MB)")
            return@withContext null
        }

        try {
            val extension = if (type == "image/png") "png" else "jpg"
            val fileName = "diary_${System.currentTimeMillis()}_${(0..1000).random()}.$extension"
            val file = File(context.filesDir, fileName)
            
            val success = context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                    true
                }
            } ?: false
            
            if (success) {
                Log.d(TAG, "Image saved: $fileName")
                fileName
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy image", e)
            null
        }
    }

    fun deleteEntry(entryWithImages: DiaryEntryWithImages) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                entryWithImages.images.forEach { diaryImage ->
                    try {
                        val file = File(context.filesDir, diaryImage.fileName)
                        if (file.exists() && file.absolutePath.contains(context.filesDir.absolutePath)) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to delete: ${diaryImage.fileName}", e)
                    }
                }
            }
            repository.deleteEntry(entryWithImages.entry)
        }
    }

    // Promise methods
    fun savePromise(content: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val promise = Promise(
                userId = userId,
                date = getStartOfDay(System.currentTimeMillis()),
                content = content,
                lastModified = System.currentTimeMillis(),
                isSynced = false
            )
            repository.insertPromise(promise)
        }
    }

    fun updatePromiseStatus(promise: Promise, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.updatePromiseStatus(promise.id, isCompleted)
        }
    }

    fun deletePromise(promise: Promise) {
        viewModelScope.launch {
            repository.deletePromise(promise)
        }
    }

    /**
     * Final save operation that runs in ViewModelScope to ensure completion 
     * even if the UI navigates away.
     */
    fun persistEntryFinal(
        title: String,
        content: String,
        images: List<Uri>,
        id: Long = 0,
        isVaultItem: Boolean = false,
        unlockDate: Long? = null,
        mood: String? = null,
        songLink: String? = null,
        songTitle: String? = null,
        songArtist: String? = null,
        songThumbnailUrl: String? = null,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            saveEntry(
                title = title,
                content = content,
                images = images,
                id = id,
                isDraft = false, // Always false for final save
                isVaultItem = isVaultItem,
                unlockDate = unlockDate,
                mood = mood,
                songLink = songLink,
                songTitle = songTitle,
                songArtist = songArtist,
                songThumbnailUrl = songThumbnailUrl
            )
            onComplete()
        }
    }
}
