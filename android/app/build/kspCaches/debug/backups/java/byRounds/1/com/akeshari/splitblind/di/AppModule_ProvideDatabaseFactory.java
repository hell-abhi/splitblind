package com.akeshari.splitblind.di;

import android.content.Context;
import com.akeshari.splitblind.data.database.SplitBlindDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class AppModule_ProvideDatabaseFactory implements Factory<SplitBlindDatabase> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideDatabaseFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public SplitBlindDatabase get() {
    return provideDatabase(contextProvider.get());
  }

  public static AppModule_ProvideDatabaseFactory create(Provider<Context> contextProvider) {
    return new AppModule_ProvideDatabaseFactory(contextProvider);
  }

  public static SplitBlindDatabase provideDatabase(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideDatabase(context));
  }
}
