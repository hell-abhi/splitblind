package com.akeshari.splitblind.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.akeshari.splitblind.data.database.converter.Converters
import com.akeshari.splitblind.data.database.dao.ExpenseDao
import com.akeshari.splitblind.data.database.dao.GroupDao
import com.akeshari.splitblind.data.database.dao.HistoryDao
import com.akeshari.splitblind.data.database.dao.ProcessedOpDao
import com.akeshari.splitblind.data.database.dao.SettlementDao
import com.akeshari.splitblind.data.database.entity.ExpenseEntity
import com.akeshari.splitblind.data.database.entity.GroupEntity
import com.akeshari.splitblind.data.database.entity.HistoryEntity
import com.akeshari.splitblind.data.database.entity.MemberEntity
import com.akeshari.splitblind.data.database.entity.ProcessedOpEntity
import com.akeshari.splitblind.data.database.entity.SettlementEntity

@Database(
    entities = [
        GroupEntity::class,
        MemberEntity::class,
        ExpenseEntity::class,
        SettlementEntity::class,
        ProcessedOpEntity::class,
        HistoryEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SplitBlindDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun settlementDao(): SettlementDao
    abstract fun processedOpDao(): ProcessedOpDao
    abstract fun historyDao(): HistoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN tag TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE expenses ADD COLUMN paidByMap TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE expenses ADD COLUMN splitMode TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE expenses ADD COLUMN splitDetails TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS history (
                        historyId TEXT NOT NULL PRIMARY KEY,
                        expenseId TEXT,
                        settlementId TEXT,
                        entityType TEXT NOT NULL,
                        action TEXT NOT NULL,
                        previousData TEXT,
                        newData TEXT,
                        changedBy TEXT NOT NULL,
                        changedByName TEXT NOT NULL,
                        changedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN notes TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `groups` ADD COLUMN groupType TEXT DEFAULT NULL")
            }
        }
    }
}
