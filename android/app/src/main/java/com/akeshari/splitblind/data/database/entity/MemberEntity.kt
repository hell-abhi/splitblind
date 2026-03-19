package com.akeshari.splitblind.data.database.entity

import androidx.room.Entity

@Entity(tableName = "members", primaryKeys = ["groupId", "memberId"])
data class MemberEntity(
    val groupId: String,
    val memberId: String,
    val displayName: String,
    val joinedAt: Long,
    val isDeleted: Boolean = false,
    val hlcTimestamp: Long
)
