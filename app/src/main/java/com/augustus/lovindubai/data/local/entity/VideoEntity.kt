package com.augustus.lovindubai.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "videos",
    indices = [
        Index(value = ["type"]),
        Index(value = ["publishedAt"])
    ]
)
data class VideoEntity(
    @PrimaryKey
    val id: String,

    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val publishedAt: String,

    val duration: String?,
    val viewCount: Long,

    // Store enum as String in Room.
    val type: String,

    val isLive: Boolean
)