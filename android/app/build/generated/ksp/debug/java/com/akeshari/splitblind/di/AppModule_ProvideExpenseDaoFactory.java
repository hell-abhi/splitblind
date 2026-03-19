package com.akeshari.splitblind.di;

import com.akeshari.splitblind.data.database.SplitBlindDatabase;
import com.akeshari.splitblind.data.database.dao.ExpenseDao;
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
public final class AppModule_ProvideExpenseDaoFactory implements Factory<ExpenseDao> {
  private final Provider<SplitBlindDatabase> dbProvider;

  public AppModule_ProvideExpenseDaoFactory(Provider<SplitBlindDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public ExpenseDao get() {
    return provideExpenseDao(dbProvider.get());
  }

  public static AppModule_ProvideExpenseDaoFactory create(Provider<SplitBlindDatabase> dbProvider) {
    return new AppModule_ProvideExpenseDaoFactory(dbProvider);
  }

  public static ExpenseDao provideExpenseDao(SplitBlindDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideExpenseDao(db));
  }
}
