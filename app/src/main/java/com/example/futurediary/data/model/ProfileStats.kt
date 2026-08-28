package com.example.futurediary.data.model

data class ProfileStats(
    val totalEntries: Int = 0,
    val totalWords: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val topWords: List<Pair<String, Int>> = emptyList(),
    val activityMap: Map<Long, Int> = emptyMap(), // Timestamp (start of day) -> Count
    val moodDistribution: Map<String, Int> = emptyMap() // Emoji -> Count
)
