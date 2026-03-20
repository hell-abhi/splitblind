package com.akeshari.splitblind.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val historyId: String,
    val expenseId: String? = null,
    val settlementId: String? = null,
    val entityType: String, // "expense" or "settlement"
    val action: String, // "created", "edited", "deleted"
    val previousData: String? = null, // JSON snapshot
    val newData: String? = null, // JSON snapshot
    val changedBy: String,
    val changedByName: String,
    val changedAt: Long
)
