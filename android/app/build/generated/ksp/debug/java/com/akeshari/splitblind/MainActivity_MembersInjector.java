package com.akeshari.splitblind;

import com.akeshari.splitblind.crypto.Identity;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<Identity> identityProvider;

  public MainActivity_MembersInjector(Provider<Identity> identityProvider) {
    this.identityProvider = identityProvider;
  }

  public static MembersInjector<MainActivity> create(Provider<Identity> identityProvider) {
    return new MainActivity_MembersInjector(identityProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectIdentity(instance, identityProvider.get());
  }

  @InjectedFieldSignature("com.akeshari.splitblind.MainActivity.identity")
  public static void injectIdentity(MainActivity instance, Identity identity) {
    instance.identity = identity;
  }
}
