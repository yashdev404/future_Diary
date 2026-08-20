package com.example.futurediary.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class DiaryEntryWithImages(
    @Embedded val entry: DiaryEntry,
    @Relation(
        parentColumn = "id",
        entityColumn = "entryId"
    )
    val images: List<DiaryImage>
)
