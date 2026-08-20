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
import com.example.futurediary.data.model.UserProfile
import com.example.futurediary.data.repository.DiaryRepository
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
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

private const val TAG = "DiaryViewModel"

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
                    entriesList.filter { item ->
                        item.entry.title.contains(query, ignoreCase = true) || 
                        item.entry.content.contains(query, ignoreCase = true)
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
    val stats: StateFlow<ProfileStats> = combine(
        entries,
        vaultEntries
    ) { mainList, vaultList ->
        val allEntriesWithImages = mainList + vaultList
        if (allEntriesWithImages.isEmpty()) return@combine ProfileStats()

        val startTime = System.currentTimeMillis()

        // 1. Word Analysis
        val wordCounts = allEntriesWithImages.asSequence()
            .flatMap { it.entry.content.split(Regex("\\W+")).asSequence() }
            .map { it.lowercase() }
            .filter { it.length > 2 && it !in STOP_WORDS }
            .groupingBy { it }
            .eachCount()

        val topWords = wordCounts.toList()
            .sortedByDescending { it.second }
            .take(5)

        val totalWords = allEntriesWithImages.sumOf { it.entry.content.split(Regex("\\W+")).size }

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
            activityMap = activityMap
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileStats()
    )

    fun setFilterDate(timestamp: Long?) {
        _filterDate.value = timestamp
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateProfile(name: String, imageUri: Uri?) {
        val userId = auth.currentUser?.uid ?: return
        val finalName = if (name.isBlank()) "Journal Owner" else name
        
        viewModelScope.launch {
            Log.d(TAG, "Updating profile for user: $userId, name: $finalName")
            var permanentImagePath: String? = userProfile.value.profileImageUri
            
            if (imageUri != null && imageUri.toString() != permanentImagePath) {
                Log.d(TAG, "New profile image detected: $imageUri")
                permanentImagePath = if (imageUri.toString().startsWith("file://") || imageUri.toString().startsWith("/")) {
                    imageUri.toString()
                } else {
                    saveImageToInternalStorage(imageUri)
                }
            }

            repository.upsertUserProfile(
                UserProfile(
                    userId = userId,
                    name = finalName,
                    profileImageUri = permanentImagePath
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

    fun getEntryById(id: Long) : Flow<DiaryEntryWithImages?> {
        val userId = auth.currentUser?.uid ?: return flowOf(null)
        return repository.getEntryById(id, userId)
    }

    suspend fun saveEntry(
        title: String,
        content: String,
        images: List<Uri>,
        id: Long = 0,
        isDraft: Boolean = false,
        isVaultItem: Boolean = false,
        unlockDate: Long? = null
    ): Long {
        val userId = auth.currentUser?.uid ?: return 0
        
        val diaryEntry = DiaryEntry(
            id = id,
            userId = userId,
            date = System.currentTimeMillis(),
            title = title,
            content = content,
            imageUri = null, // No longer used in main entries
            isDraft = isDraft,
            isVaultItem = isVaultItem,
            unlockDate = unlockDate
        )
        
        val entryId = repository.insertEntry(diaryEntry)
        
        // Handle images
        val permanentPaths = images.mapNotNull { uri ->
            if (uri.toString().startsWith("file://") || uri.toString().startsWith("/")) {
                uri.toString()
            } else {
                saveImageToInternalStorage(uri)
            }
        }
        
        val diaryImages = permanentPaths.map { path ->
            DiaryImage(entryId = entryId, imageUri = path)
        }
        
        if (diaryImages.isNotEmpty()) {
            repository.insertImages(diaryImages)
        }
        
        Log.d(TAG, "Entry saved/updated with id $entryId for user $userId (images: ${diaryImages.size})")
        return entryId
    }

    private suspend fun saveImageToInternalStorage(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val fileName = "diary_${System.currentTimeMillis()}_${(0..1000).random()}.jpg"
            val file = File(context.filesDir, fileName)
            
            val success = context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                    true
                }
            } ?: false
            
            if (success) {
                Log.d(TAG, "Image copied successfully to: ${file.absolutePath}")
                file.absolutePath
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
            // Delete all associated image files
            withContext(Dispatchers.IO) {
                entryWithImages.images.forEach { diaryImage ->
                    try {
                        val uri = diaryImage.imageUri.toUri()
                        val file = if (uri.scheme == "file" || uri.scheme == null) {
                            File(uri.path ?: diaryImage.imageUri)
                        } else {
                            null
                        }

                        if (file != null && file.exists() && file.absolutePath.contains(context.filesDir.absolutePath)) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to delete image file: ${diaryImage.imageUri}", e)
                    }
                }
            }
            repository.deleteEntry(entryWithImages.entry)
        }
    }
}