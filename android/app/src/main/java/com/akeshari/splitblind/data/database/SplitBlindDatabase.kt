package com.akeshari.splitblind.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.akeshari.splitblind.data.database.converter.Converters
import com.akeshari.splitblind.data.database.dao.ExpenseDao
import com.akeshari.splitblind.data.database.dao.GroupDao
import com.akeshari.splitblind.data.database.dao.ProcessedOpDao
import com.akeshari.splitblind.data.database.dao.SettlementDao
import com.akeshari.splitblind.data.database.entity.ExpenseEntity
import com.akeshari.splitblind.data.database.entity.GroupEntity
import com.akeshari.splitblind.data.database.entity.MemberEntity
import com.akeshari.splitblind.data.database.entity.ProcessedOpEntity
import com.akeshari.splitblind.data.database.entity.SettlementEntity

@Database(
    entities = [
        GroupEntity::class,
        MemberEntity::class,
        ExpenseEntity::class,
        SettlementEntity::class,
        ProcessedOpEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SplitBlindDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun settlementDao(): SettlementDao
    abstract fun processedOpDao(): ProcessedOpDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN tag TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE expenses ADD COLUMN paidByMap TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE expenses ADD COLUMN splitMode TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE expenses ADD COLUMN splitDetails TEXT DEFAULT NULL")
            }
        }
    }
}
