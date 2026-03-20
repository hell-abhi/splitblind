package com.akeshari.splitblind.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.akeshari.splitblind.data.database.entity.GroupEntity
import com.akeshari.splitblind.data.database.entity.MemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY createdAt DESC")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups ORDER BY createdAt DESC")
    suspend fun getAllGroupsList(): List<GroupEntity>

    @Query("SELECT * FROM groups WHERE groupId = :groupId")
    suspend fun getGroup(groupId: String): GroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity)

    @Query("SELECT * FROM members WHERE groupId = :groupId AND isDeleted = 0")
    fun getMembers(groupId: String): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE groupId = :groupId AND isDeleted = 0")
    suspend fun getMembersList(groupId: String): List<MemberEntity>

    @Query("SELECT * FROM members")
    suspend fun getAllMembersList(): List<MemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberEntity)
}
