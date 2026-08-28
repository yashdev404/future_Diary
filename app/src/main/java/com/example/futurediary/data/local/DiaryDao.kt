package com.example.futurediary.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.futurediary.data.model.DiaryEntry
import com.example.futurediary.data.model.DiaryEntryWithImages
import com.example.futurediary.data.model.DiaryImage
import com.example.futurediary.data.model.Promise
import com.example.futurediary.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {
    @Query("SELECT * FROM user_profiles WHERE userId = :userId")
    fun getUserProfile(userId: String): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProfile(profile: UserProfile)

    @Transaction
    @Query("SELECT * FROM diary_entries WHERE userId = :userId AND isDraft = 0 AND isVaultItem = 0 ORDER BY date DESC")
    fun getAllEntries(userId: String): Flow<List<DiaryEntryWithImages>>

    @Transaction
    @Query("SELECT * FROM diary_entries WHERE userId = :userId AND isDraft = 1 ORDER BY date DESC")
    fun getAllDrafts(userId: String): Flow<List<DiaryEntryWithImages>>

    @Transaction
    @Query("SELECT * FROM diary_entries WHERE userId = :userId AND isVaultItem = 1 ORDER BY date DESC")
    fun getVaultEntries(userId: String): Flow<List<DiaryEntryWithImages>>

    @Transaction
    @Query("SELECT * FROM diary_entries WHERE id = :id AND userId = :userId")
    fun getEntryById(id: Long, userId: String): Flow<DiaryEntryWithImages?>

    @Transaction
    @Query("SELECT * FROM diary_entries WHERE userId = :userId AND isDraft = 0 AND isVaultItem = 0 AND date >= :startDate AND date < :endDate ORDER BY date DESC")
    fun getEntriesByDateRange(userId: String, startDate: Long, endDate: Long): Flow<List<DiaryEntryWithImages>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: DiaryEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<DiaryImage>)

    @Query("DELETE FROM diary_images WHERE entryId = :entryId")
    suspend fun deleteImagesForEntry(entryId: Long)

    @Delete
    suspend fun deleteEntry(entry: DiaryEntry)

    // Promises
    @Query("SELECT * FROM promises WHERE userId = :userId ORDER BY date DESC")
    fun getAllPromises(userId: String): Flow<List<Promise>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromise(promise: Promise): Long

    @Query("UPDATE promises SET isCompleted = :isCompleted, lastModified = :lastModified, isSynced = 0 WHERE id = :id")
    suspend fun updatePromiseStatus(id: Long, isCompleted: Boolean, lastModified: Long)

    @Delete
    suspend fun deletePromise(promise: Promise)

    // Sync Queries
    @Query("SELECT * FROM diary_entries WHERE isSynced = 0")
    suspend fun getUnsyncedEntries(): List<DiaryEntry>

    @Query("UPDATE diary_entries SET isSynced = 1 WHERE id = :id")
    suspend fun markEntrySynced(id: Long)

    @Query("SELECT * FROM promises WHERE isSynced = 0")
    suspend fun getUnsyncedPromises(): List<Promise>

    @Query("UPDATE promises SET isSynced = 1 WHERE id = :id")
    suspend fun markPromiseSynced(id: Long)

    @Query("SELECT * FROM diary_images WHERE remoteUrl IS NULL")
    suspend fun getUnsyncedImages(): List<DiaryImage>

    @Query("UPDATE diary_images SET remoteUrl = :remoteUrl WHERE id = :id")
    suspend fun updateImageRemoteUrl(id: Long, remoteUrl: String)
}
