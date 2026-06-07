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
    fun getAllEntries(): Flow<List<DiaryEntry>> = diaryDao.getAllEntries()

    fun getEntryById(id: Long): Flow<DiaryEntry?> = diaryDao.getEntryById(id)

    suspend fun insertEntry(entry: DiaryEntry) = diaryDao.insertEntry(entry)

    suspend fun deleteEntry(entry: DiaryEntry) = diaryDao.deleteEntry(entry)
}