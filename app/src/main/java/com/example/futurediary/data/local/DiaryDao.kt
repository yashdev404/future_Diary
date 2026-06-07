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
    @Query("SELECT * FROM diary_entries ORDER BY date DESC")
    fun getAllEntries(): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE id = :id")
    fun getEntryById(id: Long): Flow<DiaryEntry?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: DiaryEntry)

    @Delete
    suspend fun deleteEntry(entry: DiaryEntry)
}