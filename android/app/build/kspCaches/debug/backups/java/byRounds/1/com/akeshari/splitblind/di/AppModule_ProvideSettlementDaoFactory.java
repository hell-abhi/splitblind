package com.akeshari.splitblind.di;

import com.akeshari.splitblind.data.database.SplitBlindDatabase;
import com.akeshari.splitblind.data.database.dao.SettlementDao;
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
public final class AppModule_ProvideSettlementDaoFactory implements Factory<SettlementDao> {
  private final Provider<SplitBlindDatabase> dbProvider;

  public AppModule_ProvideSettlementDaoFactory(Provider<SplitBlindDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public SettlementDao get() {
    return provideSettlementDao(dbProvider.get());
  }

  public static AppModule_ProvideSettlementDaoFactory create(
      Provider<SplitBlindDatabase> dbProvider) {
    return new AppModule_ProvideSettlementDaoFactory(dbProvider);
  }

  public static SettlementDao provideSettlementDao(SplitBlindDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideSettlementDao(db));
  }
}
