package com.akeshari.splitblind.di;

import com.akeshari.splitblind.data.database.SplitBlindDatabase;
import com.akeshari.splitblind.data.database.dao.ProcessedOpDao;
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
public final class AppModule_ProvideProcessedOpDaoFactory implements Factory<ProcessedOpDao> {
  private final Provider<SplitBlindDatabase> dbProvider;

  public AppModule_ProvideProcessedOpDaoFactory(Provider<SplitBlindDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public ProcessedOpDao get() {
    return provideProcessedOpDao(dbProvider.get());
  }

  public static AppModule_ProvideProcessedOpDaoFactory create(
      Provider<SplitBlindDatabase> dbProvider) {
    return new AppModule_ProvideProcessedOpDaoFactory(dbProvider);
  }

  public static ProcessedOpDao provideProcessedOpDao(SplitBlindDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideProcessedOpDao(db));
  }
}
