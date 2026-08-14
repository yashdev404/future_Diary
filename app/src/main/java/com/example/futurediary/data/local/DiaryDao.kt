package com.example.futurediary.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.futurediary.data.model.DiaryEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary_entries WHERE userId = :userId AND isDraft = 0 AND isVaultItem = 0 ORDER BY date DESC")
    fun getAllEntries(userId: String): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE userId = :userId AND isDraft = 1 ORDER BY date DESC")
    fun getAllDrafts(userId: String): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE userId = :userId AND isVaultItem = 1 ORDER BY date DESC")
    fun getVaultEntries(userId: String): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE id = :id AND userId = :userId")
    fun getEntryById(id: Long, userId: String): Flow<DiaryEntry?>

    @Query("SELECT * FROM diary_entries WHERE userId = :userId AND isDraft = 0 AND isVaultItem = 0 AND date >= :startDate AND date < :endDate ORDER BY date DESC")
    fun getEntriesByDateRange(userId: String, startDate: Long, endDate: Long): Flow<List<DiaryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: DiaryEntry): Long

    @Delete
    suspend fun deleteEntry(entry: DiaryEntry)
}