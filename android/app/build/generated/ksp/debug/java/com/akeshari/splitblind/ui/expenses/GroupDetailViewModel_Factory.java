package com.akeshari.splitblind.ui.expenses;

import androidx.lifecycle.SavedStateHandle;
import com.akeshari.splitblind.crypto.Identity;
import com.akeshari.splitblind.data.database.dao.ExpenseDao;
import com.akeshari.splitblind.data.database.dao.GroupDao;
import com.akeshari.splitblind.data.database.dao.SettlementDao;
import com.akeshari.splitblind.sync.SyncEngine;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class GroupDetailViewModel_Factory implements Factory<GroupDetailViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<GroupDao> groupDaoProvider;

  private final Provider<ExpenseDao> expenseDaoProvider;

  private final Provider<SettlementDao> settlementDaoProvider;

  private final Provider<SyncEngine> syncEngineProvider;

  private final Provider<Identity> identityProvider;

  public GroupDetailViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<GroupDao> groupDaoProvider, Provider<ExpenseDao> expenseDaoProvider,
      Provider<SettlementDao> settlementDaoProvider, Provider<SyncEngine> syncEngineProvider,
      Provider<Identity> identityProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.groupDaoProvider = groupDaoProvider;
    this.expenseDaoProvider = expenseDaoProvider;
    this.settlementDaoProvider = settlementDaoProvider;
    this.syncEngineProvider = syncEngineProvider;
    this.identityProvider = identityProvider;
  }

  @Override
  public GroupDetailViewModel get() {
    return newInstance(savedStateHandleProvider.get(), groupDaoProvider.get(), expenseDaoProvider.get(), settlementDaoProvider.get(), syncEngineProvider.get(), identityProvider.get());
  }

  public static GroupDetailViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider, Provider<GroupDao> groupDaoProvider,
      Provider<ExpenseDao> expenseDaoProvider, Provider<SettlementDao> settlementDaoProvider,
      Provider<SyncEngine> syncEngineProvider, Provider<Identity> identityProvider) {
    return new GroupDetailViewModel_Factory(savedStateHandleProvider, groupDaoProvider, expenseDaoProvider, settlementDaoProvider, syncEngineProvider, identityProvider);
  }

  public static GroupDetailViewModel newInstance(SavedStateHandle savedStateHandle,
      GroupDao groupDao, ExpenseDao expenseDao, SettlementDao settlementDao, SyncEngine syncEngine,
      Identity identity) {
    return new GroupDetailViewModel(savedStateHandle, groupDao, expenseDao, settlementDao, syncEngine, identity);
  }
}
