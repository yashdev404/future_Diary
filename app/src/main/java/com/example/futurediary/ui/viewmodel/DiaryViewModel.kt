package com.example.futurediary.ui.viewmodel

import android.util.Log
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.futurediary.data.model.DiaryEntry
import com.example.futurediary.data.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    // This turns the database Flow into a StateFlow that the UI can easily read
    val entries: StateFlow<List<DiaryEntry>> = repository.getAllEntries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getEntryById(id: Long) = repository.getEntryById(id)

    fun saveEntry(title: String, content: String, imageUri: String?, id: Long = 0) {
        viewModelScope.launch {
            var permanentImagePath: String? = null
            
            if (imageUri != null) {
                // If the URI is already a local file path (from internal storage), don't copy it again
                permanentImagePath = if (imageUri.startsWith("file://") || imageUri.startsWith("/")) {
                    imageUri
                } else {
                    saveImageToInternalStorage(Uri.parse(imageUri))
                }
            }

            val newEntry = DiaryEntry(
                id = id,
                date = System.currentTimeMillis(),
                title = title,
                content = content,
                imageUri = permanentImagePath
            )
            repository.insertEntry(newEntry)
            Log.d(TAG, "Entry saved/updated with id $id and image path: $permanentImagePath")
        }
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
            repository.deleteEntry(entry)
        }
    }
}