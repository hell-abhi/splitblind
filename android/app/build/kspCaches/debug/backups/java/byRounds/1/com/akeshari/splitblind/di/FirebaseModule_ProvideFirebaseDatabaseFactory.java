package com.akeshari.splitblind.di;

import android.content.Context;
import com.google.firebase.database.FirebaseDatabase;
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
public final class FirebaseModule_ProvideFirebaseDatabaseFactory implements Factory<FirebaseDatabase> {
  private final Provider<Context> contextProvider;

  public FirebaseModule_ProvideFirebaseDatabaseFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public FirebaseDatabase get() {
    return provideFirebaseDatabase(contextProvider.get());
  }

  public static FirebaseModule_ProvideFirebaseDatabaseFactory create(
      Provider<Context> contextProvider) {
    return new FirebaseModule_ProvideFirebaseDatabaseFactory(contextProvider);
  }

  public static FirebaseDatabase provideFirebaseDatabase(Context context) {
    return Preconditions.checkNotNullFromProvides(FirebaseModule.INSTANCE.provideFirebaseDatabase(context));
  }
}
