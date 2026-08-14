package com.example.futurediary.data.repository

import com.example.futurediary.data.local.DiaryDao
import com.example.futurediary.data.model.DiaryEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiaryRepository @Inject constructor(
    private val diaryDao: DiaryDao
) {
    fun getAllEntries(userId: String): Flow<List<DiaryEntry>> = diaryDao.getAllEntries(userId)

    fun getEntryById(id: Long, userId: String): Flow<DiaryEntry?> = diaryDao.getEntryById(id, userId)

    fun getEntriesByDateRange(userId: String, start: Long, end: Long): Flow<List<DiaryEntry>> = 
        diaryDao.getEntriesByDateRange(userId, start, end)

    fun getAllDrafts(userId: String): Flow<List<DiaryEntry>> = diaryDao.getAllDrafts(userId)

    fun getVaultEntries(userId: String): Flow<List<DiaryEntry>> = diaryDao.getVaultEntries(userId)

    suspend fun insertEntry(entry: DiaryEntry): Long = diaryDao.insertEntry(entry)

    suspend fun deleteEntry(entry: DiaryEntry) = diaryDao.deleteEntry(entry)
}