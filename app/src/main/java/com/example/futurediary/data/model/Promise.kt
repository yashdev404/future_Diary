package com.example.futurediary.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "promises")
data class Promise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val date: Long,
    val content: String,
    val isCompleted: Boolean = false,
    val lastModified: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
