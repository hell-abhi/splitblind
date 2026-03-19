package com.akeshari.splitblind.sync;

import com.akeshari.splitblind.data.database.dao.ExpenseDao;
import com.akeshari.splitblind.data.database.dao.GroupDao;
import com.akeshari.splitblind.data.database.dao.ProcessedOpDao;
import com.akeshari.splitblind.data.database.dao.SettlementDao;
import com.google.firebase.database.FirebaseDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class SyncEngine_Factory implements Factory<SyncEngine> {
  private final Provider<FirebaseDatabase> firebaseDatabaseProvider;

  private final Provider<GroupDao> groupDaoProvider;

  private final Provider<ExpenseDao> expenseDaoProvider;

  private final Provider<SettlementDao> settlementDaoProvider;

  private final Provider<ProcessedOpDao> processedOpDaoProvider;

  public SyncEngine_Factory(Provider<FirebaseDatabase> firebaseDatabaseProvider,
      Provider<GroupDao> groupDaoProvider, Provider<ExpenseDao> expenseDaoProvider,
      Provider<SettlementDao> settlementDaoProvider,
      Provider<ProcessedOpDao> processedOpDaoProvider) {
    this.firebaseDatabaseProvider = firebaseDatabaseProvider;
    this.groupDaoProvider = groupDaoProvider;
    this.expenseDaoProvider = expenseDaoProvider;
    this.settlementDaoProvider = settlementDaoProvider;
    this.processedOpDaoProvider = processedOpDaoProvider;
  }

  @Override
  public SyncEngine get() {
    return newInstance(firebaseDatabaseProvider.get(), groupDaoProvider.get(), expenseDaoProvider.get(), settlementDaoProvider.get(), processedOpDaoProvider.get());
  }

  public static SyncEngine_Factory create(Provider<FirebaseDatabase> firebaseDatabaseProvider,
      Provider<GroupDao> groupDaoProvider, Provider<ExpenseDao> expenseDaoProvider,
      Provider<SettlementDao> settlementDaoProvider,
      Provider<ProcessedOpDao> processedOpDaoProvider) {
    return new SyncEngine_Factory(firebaseDatabaseProvider, groupDaoProvider, expenseDaoProvider, settlementDaoProvider, processedOpDaoProvider);
  }

  public static SyncEngine newInstance(FirebaseDatabase firebaseDatabase, GroupDao groupDao,
      ExpenseDao expenseDao, SettlementDao settlementDao, ProcessedOpDao processedOpDao) {
    return new SyncEngine(firebaseDatabase, groupDao, expenseDao, settlementDao, processedOpDao);
  }
}
