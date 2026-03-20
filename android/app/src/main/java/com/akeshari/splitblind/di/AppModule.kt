package com.akeshari.splitblind.di

import android.content.Context
import androidx.room.Room
import com.akeshari.splitblind.crypto.Identity
import com.akeshari.splitblind.data.database.SplitBlindDatabase
import com.akeshari.splitblind.data.database.dao.ExpenseDao
import com.akeshari.splitblind.data.database.dao.GroupDao
import com.akeshari.splitblind.data.database.dao.HistoryDao
import com.akeshari.splitblind.data.database.dao.ProcessedOpDao
import com.akeshari.splitblind.data.database.dao.SettlementDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SplitBlindDatabase {
        return Room.databaseBuilder(
            context,
            SplitBlindDatabase::class.java,
            "splitblind.db"
        ).addMigrations(SplitBlindDatabase.MIGRATION_1_2, SplitBlindDatabase.MIGRATION_2_3, SplitBlindDatabase.MIGRATION_3_4).build()
    }

    @Provides
    fun provideGroupDao(db: SplitBlindDatabase): GroupDao = db.groupDao()

    @Provides
    fun provideExpenseDao(db: SplitBlindDatabase): ExpenseDao = db.expenseDao()

    @Provides
    fun provideSettlementDao(db: SplitBlindDatabase): SettlementDao = db.settlementDao()

    @Provides
    fun provideProcessedOpDao(db: SplitBlindDatabase): ProcessedOpDao = db.processedOpDao()

    @Provides
    fun provideHistoryDao(db: SplitBlindDatabase): HistoryDao = db.historyDao()

    @Provides
    @Singleton
    fun provideIdentity(@ApplicationContext context: Context): Identity {
        return Identity(context)
    }
}
