package com.example.futurediary.data.repository

import com.example.futurediary.data.local.DiaryDao
import com.example.futurediary.data.model.DiaryEntry
import com.example.futurediary.data.model.DiaryEntryWithImages
import com.example.futurediary.data.model.DiaryImage
import com.example.futurediary.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiaryRepository @Inject constructor(
    private val diaryDao: DiaryDao
) {
    fun getAllEntries(userId: String): Flow<List<DiaryEntryWithImages>> = diaryDao.getAllEntries(userId)

    fun getUserProfile(userId: String): Flow<UserProfile?> = diaryDao.getUserProfile(userId)

    suspend fun upsertUserProfile(profile: UserProfile) = diaryDao.upsertUserProfile(profile)

    fun getEntryById(id: Long, userId: String): Flow<DiaryEntryWithImages?> = diaryDao.getEntryById(id, userId)

    fun getEntriesByDateRange(userId: String, start: Long, end: Long): Flow<List<DiaryEntryWithImages>> = 
        diaryDao.getEntriesByDateRange(userId, start, end)

    fun getAllDrafts(userId: String): Flow<List<DiaryEntryWithImages>> = diaryDao.getAllDrafts(userId)

    fun getVaultEntries(userId: String): Flow<List<DiaryEntryWithImages>> = diaryDao.getVaultEntries(userId)

    suspend fun insertEntry(entry: DiaryEntry): Long = diaryDao.insertEntry(entry)

    suspend fun insertImages(images: List<DiaryImage>) = diaryDao.insertImages(images)

    suspend fun deleteImagesForEntry(entryId: Long) = diaryDao.deleteImagesForEntry(entryId)

    suspend fun deleteEntry(entry: DiaryEntry) = diaryDao.deleteEntry(entry)
}