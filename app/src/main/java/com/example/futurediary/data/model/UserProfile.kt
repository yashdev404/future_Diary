package com.example.futurediary.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey
    val userId: String,
    val name: String = "Journal Owner",
    val profileImageUri: String? = null
)
