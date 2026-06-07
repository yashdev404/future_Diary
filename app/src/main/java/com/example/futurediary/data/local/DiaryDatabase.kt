package com.example.futurediary.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.futurediary.data.model.DiaryEntry

@Database(entities = [DiaryEntry::class], version = 1, exportSchema = false)
abstract class DiaryDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao
}