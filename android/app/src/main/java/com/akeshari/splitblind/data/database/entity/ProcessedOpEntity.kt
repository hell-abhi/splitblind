package com.akeshari.splitblind.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "processed_ops")
data class ProcessedOpEntity(
    @PrimaryKey val opId: String,
    val processedAt: Long = System.currentTimeMillis()
)
