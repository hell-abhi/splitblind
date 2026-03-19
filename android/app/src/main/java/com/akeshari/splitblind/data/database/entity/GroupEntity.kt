package com.akeshari.splitblind.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val groupId: String,
    val name: String,
    val createdBy: String,
    val createdAt: Long,
    val groupKeyBase64: String,
    val hlcTimestamp: Long
)
