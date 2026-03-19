package com.akeshari.splitblind.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.akeshari.splitblind.data.database.entity.ProcessedOpEntity

@Dao
interface ProcessedOpDao {
    @Query("SELECT EXISTS(SELECT 1 FROM processed_ops WHERE opId = :opId)")
    suspend fun isProcessed(opId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markProcessed(op: ProcessedOpEntity)
}
