package com.akeshari.splitblind.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.akeshari.splitblind.data.database.entity.SettlementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettlementDao {
    @Query("SELECT * FROM settlements WHERE groupId = :groupId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getSettlements(groupId: String): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements WHERE groupId = :groupId AND isDeleted = 0")
    suspend fun getSettlementsList(groupId: String): List<SettlementEntity>

    @Query("SELECT * FROM settlements WHERE settlementId = :settlementId")
    suspend fun getSettlement(settlementId: String): SettlementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: SettlementEntity)

    @Query("SELECT * FROM settlements WHERE groupId = :groupId ORDER BY createdAt DESC")
    fun getAllSettlementsIncludingDeleted(groupId: String): Flow<List<SettlementEntity>>
}
