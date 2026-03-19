package com.akeshari.splitblind.ui.settle;

import androidx.lifecycle.SavedStateHandle;
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
public final class SettleUpViewModel_Factory implements Factory<SettleUpViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<GroupDao> groupDaoProvider;

  private final Provider<SettlementDao> settlementDaoProvider;

  private final Provider<SyncEngine> syncEngineProvider;

  public SettleUpViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<GroupDao> groupDaoProvider, Provider<SettlementDao> settlementDaoProvider,
      Provider<SyncEngine> syncEngineProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.groupDaoProvider = groupDaoProvider;
    this.settlementDaoProvider = settlementDaoProvider;
    this.syncEngineProvider = syncEngineProvider;
  }

  @Override
  public SettleUpViewModel get() {
    return newInstance(savedStateHandleProvider.get(), groupDaoProvider.get(), settlementDaoProvider.get(), syncEngineProvider.get());
  }

  public static SettleUpViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider, Provider<GroupDao> groupDaoProvider,
      Provider<SettlementDao> settlementDaoProvider, Provider<SyncEngine> syncEngineProvider) {
    return new SettleUpViewModel_Factory(savedStateHandleProvider, groupDaoProvider, settlementDaoProvider, syncEngineProvider);
  }

  public static SettleUpViewModel newInstance(SavedStateHandle savedStateHandle, GroupDao groupDao,
      SettlementDao settlementDao, SyncEngine syncEngine) {
    return new SettleUpViewModel(savedStateHandle, groupDao, settlementDao, syncEngine);
  }
}
