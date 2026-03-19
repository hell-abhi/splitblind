package com.akeshari.splitblind.ui.onboarding;

import com.akeshari.splitblind.crypto.Identity;
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
public final class OnboardingViewModel_Factory implements Factory<OnboardingViewModel> {
  private final Provider<Identity> identityProvider;

  public OnboardingViewModel_Factory(Provider<Identity> identityProvider) {
    this.identityProvider = identityProvider;
  }

  @Override
  public OnboardingViewModel get() {
    return newInstance(identityProvider.get());
  }

  public static OnboardingViewModel_Factory create(Provider<Identity> identityProvider) {
    return new OnboardingViewModel_Factory(identityProvider);
  }

  public static OnboardingViewModel newInstance(Identity identity) {
    return new OnboardingViewModel(identity);
  }
}
