package com.example.futurediary.ui.viewmodel

import android.util.Log
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.futurediary.data.model.DiaryEntry
import com.example.futurediary.data.repository.DiaryRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

private const val TAG = "DiaryViewModel"

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val repository: DiaryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val _currentUserId = MutableStateFlow(auth.currentUser?.uid)
    private val _filterDate = MutableStateFlow<Long?>(null) // null means show all

    @OptIn(ExperimentalCoroutinesApi::class)
    val entries: StateFlow<List<DiaryEntry>> = kotlinx.coroutines.flow.combine(
        _currentUserId,
        _filterDate
    ) { userId, filterDate ->
        userId to filterDate
    }.flatMapLatest { (userId, filterDate) ->
        if (userId != null) {
            if (filterDate == null) {
                repository.getAllEntries(userId)
            } else {
                val startOfDay = getStartOfDay(filterDate)
                val endOfDay = startOfDay + 86400000L
                repository.getEntriesByDateRange(userId, startOfDay, endOfDay)
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
    val drafts: StateFlow<List<DiaryEntry>> = _currentUserId
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
    val vaultEntries: StateFlow<List<DiaryEntry>> = _currentUserId
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
    val photosEntries: StateFlow<List<DiaryEntry>> = kotlinx.coroutines.flow.combine(
        entries,
        vaultEntries
    ) { entriesList, vaultList ->
        // Combine both main entries and vault entries, then filter for those with photos
        (entriesList + vaultList).filter { it.imageUri != null }
            .sortedByDescending { it.date }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setFilterDate(timestamp: Long?) {
        _filterDate.value = timestamp
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

    fun getEntryById(id: Long) : kotlinx.coroutines.flow.Flow<DiaryEntry?> {
        val userId = auth.currentUser?.uid ?: return flowOf(null)
        return repository.getEntryById(id, userId)
    }

    suspend fun saveEntry(
        title: String,
        content: String,
        imageUri: String?,
        id: Long = 0,
        isDraft: Boolean = false,
        isVaultItem: Boolean = false,
        unlockDate: Long? = null
    ): Long {
        val userId = auth.currentUser?.uid ?: return 0
        
        var permanentImagePath: String? = null
        
        if (imageUri != null) {
            permanentImagePath = if (imageUri.startsWith("file://") || imageUri.startsWith("/")) {
                imageUri
            } else {
                saveImageToInternalStorage(Uri.parse(imageUri))
            }
        }

        val newEntry = DiaryEntry(
            id = id,
            userId = userId,
            date = System.currentTimeMillis(),
            title = title,
            content = content,
            imageUri = permanentImagePath,
            isDraft = isDraft,
            isVaultItem = isVaultItem,
            unlockDate = unlockDate
        )
        val newId = repository.insertEntry(newEntry)
        Log.d(TAG, "Entry saved/updated with id $newId for user $userId (isDraft: $isDraft, isVault: $isVaultItem)")
        return newId
    }

    private suspend fun saveImageToInternalStorage(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val fileName = "diary_${System.currentTimeMillis()}.jpg"
            val file = File(context.filesDir, fileName)
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Image copied successfully to: ${file.absolutePath}")
            return@withContext file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy image", e)
            null
        }
    }

    fun deleteEntry(entry: DiaryEntry) {
        viewModelScope.launch {
            // Delete associated image file if it exists in internal storage
            entry.imageUri?.let { uriString ->
                withContext(Dispatchers.IO) {
                    try {
                        val uri = Uri.parse(uriString)
                        val file = if (uri.scheme == "file" || uri.scheme == null) {
                            File(uri.path ?: uriString)
                        } else {
                            null
                        }

                        // Only delete if the file is inside our app's internal files directory
                        if (file != null && file.exists() && file.absolutePath.contains(context.filesDir.absolutePath)) {
                            val deleted = file.delete()
                            Log.d(TAG, "Image file deleted: ${file.absolutePath}, success: $deleted")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to delete image file: $uriString", e)
                    }
                }
            }
            repository.deleteEntry(entry)
        }
    }
}