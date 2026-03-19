package com.akeshari.splitblind.di;

import com.akeshari.splitblind.data.database.SplitBlindDatabase;
import com.akeshari.splitblind.data.database.dao.GroupDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideGroupDaoFactory implements Factory<GroupDao> {
  private final Provider<SplitBlindDatabase> dbProvider;

  public AppModule_ProvideGroupDaoFactory(Provider<SplitBlindDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public GroupDao get() {
    return provideGroupDao(dbProvider.get());
  }

  public static AppModule_ProvideGroupDaoFactory create(Provider<SplitBlindDatabase> dbProvider) {
    return new AppModule_ProvideGroupDaoFactory(dbProvider);
  }

  public static GroupDao provideGroupDao(SplitBlindDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideGroupDao(db));
  }
}
