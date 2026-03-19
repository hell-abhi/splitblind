package com.akeshari.splitblind.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settlements")
data class SettlementEntity(
    @PrimaryKey val settlementId: String,
    val groupId: String,
    val fromMember: String,
    val toMember: String,
    val amountCents: Long,
    val createdAt: Long,
    val isDeleted: Boolean = false,
    val hlcTimestamp: Long
)
