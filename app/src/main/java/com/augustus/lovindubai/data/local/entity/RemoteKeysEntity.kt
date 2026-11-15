
package com.augustus.lovindubai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_keys")
data class RemoteKeysEntity(
    @PrimaryKey
    val videoId: String,
    val prevKey: String?,
    val nextKey: String?
)
