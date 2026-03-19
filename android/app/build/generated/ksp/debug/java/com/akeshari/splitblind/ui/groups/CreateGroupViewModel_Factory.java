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
public final class CreateGroupViewModel_Factory implements Factory<CreateGroupViewModel> {
  private final Provider<GroupDao> groupDaoProvider;

  private final Provider<Identity> identityProvider;

  private final Provider<SyncEngine> syncEngineProvider;

  public CreateGroupViewModel_Factory(Provider<GroupDao> groupDaoProvider,
      Provider<Identity> identityProvider, Provider<SyncEngine> syncEngineProvider) {
    this.groupDaoProvider = groupDaoProvider;
    this.identityProvider = identityProvider;
    this.syncEngineProvider = syncEngineProvider;
  }

  @Override
  public CreateGroupViewModel get() {
    return newInstance(groupDaoProvider.get(), identityProvider.get(), syncEngineProvider.get());
  }

  public static CreateGroupViewModel_Factory create(Provider<GroupDao> groupDaoProvider,
      Provider<Identity> identityProvider, Provider<SyncEngine> syncEngineProvider) {
    return new CreateGroupViewModel_Factory(groupDaoProvider, identityProvider, syncEngineProvider);
  }

  public static CreateGroupViewModel newInstance(GroupDao groupDao, Identity identity,
      SyncEngine syncEngine) {
    return new CreateGroupViewModel(groupDao, identity, syncEngine);
  }
}
