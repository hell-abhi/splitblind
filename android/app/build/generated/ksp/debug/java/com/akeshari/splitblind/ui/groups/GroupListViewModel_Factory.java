package com.akeshari.splitblind.ui.groups;

import com.akeshari.splitblind.crypto.Identity;
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
public final class GroupListViewModel_Factory implements Factory<GroupListViewModel> {
  private final Provider<GroupDao> groupDaoProvider;

  private final Provider<Identity> identityProvider;

  private final Provider<SyncEngine> syncEngineProvider;

  public GroupListViewModel_Factory(Provider<GroupDao> groupDaoProvider,
      Provider<Identity> identityProvider, Provider<SyncEngine> syncEngineProvider) {
    this.groupDaoProvider = groupDaoProvider;
    this.identityProvider = identityProvider;
    this.syncEngineProvider = syncEngineProvider;
  }

  @Override
  public GroupListViewModel get() {
    return newInstance(groupDaoProvider.get(), identityProvider.get(), syncEngineProvider.get());
  }

  public static GroupListViewModel_Factory create(Provider<GroupDao> groupDaoProvider,
      Provider<Identity> identityProvider, Provider<SyncEngine> syncEngineProvider) {
    return new GroupListViewModel_Factory(groupDaoProvider, identityProvider, syncEngineProvider);
  }

  public static GroupListViewModel newInstance(GroupDao groupDao, Identity identity,
      SyncEngine syncEngine) {
    return new GroupListViewModel(groupDao, identity, syncEngine);
  }
}
