package com.akeshari.splitblind.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.akeshari.splitblind.data.database.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE groupId = :groupId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getExpenses(groupId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE groupId = :groupId AND isDeleted = 0")
    suspend fun getExpensesList(groupId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE expenseId = :expenseId")
    suspend fun getExpense(expenseId: String): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY createdAt DESC")
    fun getAllExpensesIncludingDeleted(groupId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllActiveExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE isDeleted = 0 ORDER BY createdAt DESC")
    suspend fun getAllActiveExpensesList(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE isDeleted = 0 ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentExpenses(limit: Int): Flow<List<ExpenseEntity>>
}
