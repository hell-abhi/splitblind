package com.akeshari.splitblind.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.akeshari.splitblind.data.database.entity.HistoryEntity

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history WHERE expenseId = :expenseId ORDER BY changedAt DESC")
    suspend fun getExpenseHistory(expenseId: String): List<HistoryEntity>

    @Query("SELECT * FROM history WHERE settlementId = :settlementId ORDER BY changedAt DESC")
    suspend fun getSettlementHistory(settlementId: String): List<HistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: HistoryEntity)

    @Query("SELECT * FROM history WHERE expenseId IN (:expenseIds) ORDER BY changedAt DESC")
    suspend fun getHistoryForExpenses(expenseIds: List<String>): List<HistoryEntity>

    @Query("SELECT * FROM history WHERE settlementId IN (:settlementIds) ORDER BY changedAt DESC")
    suspend fun getHistoryForSettlements(settlementIds: List<String>): List<HistoryEntity>
}
