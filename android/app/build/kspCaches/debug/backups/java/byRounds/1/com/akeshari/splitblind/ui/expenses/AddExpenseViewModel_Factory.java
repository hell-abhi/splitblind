package com.akeshari.splitblind.ui.expenses;

import androidx.lifecycle.SavedStateHandle;
import com.akeshari.splitblind.crypto.Identity;
import com.akeshari.splitblind.data.database.dao.ExpenseDao;
import com.akeshari.splitblind.data.database.dao.GroupDao;
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
public final class AddExpenseViewModel_Factory implements Factory<AddExpenseViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<GroupDao> groupDaoProvider;

  private final Provider<ExpenseDao> expenseDaoProvider;

  private final Provider<Identity> identityProvider;

  private final Provider<SyncEngine> syncEngineProvider;

  public AddExpenseViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<GroupDao> groupDaoProvider, Provider<ExpenseDao> expenseDaoProvider,
      Provider<Identity> identityProvider, Provider<SyncEngine> syncEngineProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.groupDaoProvider = groupDaoProvider;
    this.expenseDaoProvider = expenseDaoProvider;
    this.identityProvider = identityProvider;
    this.syncEngineProvider = syncEngineProvider;
  }

  @Override
  public AddExpenseViewModel get() {
    return newInstance(savedStateHandleProvider.get(), groupDaoProvider.get(), expenseDaoProvider.get(), identityProvider.get(), syncEngineProvider.get());
  }

  public static AddExpenseViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider, Provider<GroupDao> groupDaoProvider,
      Provider<ExpenseDao> expenseDaoProvider, Provider<Identity> identityProvider,
      Provider<SyncEngine> syncEngineProvider) {
    return new AddExpenseViewModel_Factory(savedStateHandleProvider, groupDaoProvider, expenseDaoProvider, identityProvider, syncEngineProvider);
  }

  public static AddExpenseViewModel newInstance(SavedStateHandle savedStateHandle,
      GroupDao groupDao, ExpenseDao expenseDao, Identity identity, SyncEngine syncEngine) {
    return new AddExpenseViewModel(savedStateHandle, groupDao, expenseDao, identity, syncEngine);
  }
}
