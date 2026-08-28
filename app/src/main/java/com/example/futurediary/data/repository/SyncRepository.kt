package com.example.futurediary.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.futurediary.data.model.DiaryEntry
import com.example.futurediary.data.model.Promise
import com.example.futurediary.ui.util.SecurityManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SyncRepository"

@Singleton
class SyncRepository @Inject constructor(
    private val diaryRepository: DiaryRepository,
    @ApplicationContext private val context: Context
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val securityManager = SecurityManager(context)

    suspend fun sync() {
        if (!securityManager.isCloudSyncEnabled) {
            Log.d(TAG, "Sync aborted: Cloud Backup is disabled in settings")
            return
        }
        
        val userId = auth.currentUser?.uid ?: return
        Log.d(TAG, "Starting sync for user: $userId")

        syncEntries(userId)
        syncPromises(userId)
        syncImages(userId)
    }

    private suspend fun syncEntries(userId: String) {
        val unsynced = diaryRepository.getUnsyncedEntries()
        Log.d(TAG, "Found ${unsynced.size} unsynced entries")
        
        for (entry in unsynced) {
            try {
                firestore.collection("users")
                    .document(userId)
                    .collection("entries")
                    .document(entry.id.toString())
                    .set(entry.copy(isSynced = true))
                    .await()
                
                diaryRepository.markEntrySynced(entry.id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync entry ${entry.id}", e)
            }
        }
    }

    private suspend fun syncPromises(userId: String) {
        val unsynced = diaryRepository.getUnsyncedPromises()
        Log.d(TAG, "Found ${unsynced.size} unsynced promises")

        for (promise in unsynced) {
            try {
                firestore.collection("users")
                    .document(userId)
                    .collection("promises")
                    .document(promise.id.toString())
                    .set(promise.copy(isSynced = true))
                    .await()

                diaryRepository.markPromiseSynced(promise.id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync promise ${promise.id}", e)
            }
        }
    }

    private suspend fun syncImages(userId: String) {
        val unsynced = diaryRepository.getUnsyncedImages()
        Log.d(TAG, "Found ${unsynced.size} unsynced images")

        for (image in unsynced) {
            try {
                val file = File(context.filesDir, image.fileName)
                if (!file.exists()) continue

                val storageRef = storage.reference
                    .child("users/$userId/images/${image.fileName}")
                
                storageRef.putFile(Uri.fromFile(file)).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()
                
                diaryRepository.updateImageRemoteUrl(image.id, downloadUrl)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync image ${image.id}", e)
            }
        }
    }
}
