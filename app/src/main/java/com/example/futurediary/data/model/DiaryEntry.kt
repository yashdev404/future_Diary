package com.example.futurediary.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long,
    val title: String,
    val content: String,
    val imageUri: String? = null
)