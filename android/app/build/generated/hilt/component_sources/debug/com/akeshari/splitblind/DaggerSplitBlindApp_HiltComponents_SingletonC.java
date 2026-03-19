package com.akeshari.splitblind;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.akeshari.splitblind.crypto.Identity;
import com.akeshari.splitblind.data.database.SplitBlindDatabase;
import com.akeshari.splitblind.data.database.dao.ExpenseDao;
import com.akeshari.splitblind.data.database.dao.GroupDao;
import com.akeshari.splitblind.data.database.dao.ProcessedOpDao;
import com.akeshari.splitblind.data.database.dao.SettlementDao;
import com.akeshari.splitblind.di.AppModule_ProvideDatabaseFactory;
import com.akeshari.splitblind.di.AppModule_ProvideExpenseDaoFactory;
import com.akeshari.splitblind.di.AppModule_ProvideGroupDaoFactory;
import com.akeshari.splitblind.di.AppModule_ProvideIdentityFactory;
import com.akeshari.splitblind.di.AppModule_ProvideProcessedOpDaoFactory;
import com.akeshari.splitblind.di.AppModule_ProvideSettlementDaoFactory;
import com.akeshari.splitblind.di.FirebaseModule_ProvideFirebaseDatabaseFactory;
import com.akeshari.splitblind.sync.SyncEngine;
import com.akeshari.splitblind.ui.expenses.AddExpenseViewModel;
import com.akeshari.splitblind.ui.expenses.AddExpenseViewModel_HiltModules;
import com.akeshari.splitblind.ui.expenses.AddExpenseViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.akeshari.splitblind.ui.expenses.AddExpenseViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.akeshari.splitblind.ui.expenses.GroupDetailViewModel;
import com.akeshari.splitblind.ui.expenses.GroupDetailViewModel_HiltModules;
import com.akeshari.splitblind.ui.expenses.GroupDetailViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.akeshari.splitblind.ui.expenses.GroupDetailViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.akeshari.splitblind.ui.groups.CreateGroupViewModel;
import com.akeshari.splitblind.ui.groups.CreateGroupViewModel_HiltModules;
import com.akeshari.splitblind.ui.groups.CreateGroupViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.akeshari.splitblind.ui.groups.CreateGroupViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.akeshari.splitblind.ui.groups.GroupListViewModel;
import com.akeshari.splitblind.ui.groups.GroupListViewModel_HiltModules;
import com.akeshari.splitblind.ui.groups.GroupListViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.akeshari.splitblind.ui.groups.GroupListViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.akeshari.splitblind.ui.onboarding.OnboardingViewModel;
import com.akeshari.splitblind.ui.onboarding.OnboardingViewModel_HiltModules;
import com.akeshari.splitblind.ui.onboarding.OnboardingViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.akeshari.splitblind.ui.onboarding.OnboardingViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.akeshari.splitblind.ui.settle.SettleUpViewModel;
import com.akeshari.splitblind.ui.settle.SettleUpViewModel_HiltModules;
import com.akeshari.splitblind.ui.settle.SettleUpViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.akeshari.splitblind.ui.settle.SettleUpViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.firebase.database.FirebaseDatabase;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.MapBuilder;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

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
public final class DaggerSplitBlindApp_HiltComponents_SingletonC {
  private DaggerSplitBlindApp_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public SplitBlindApp_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements SplitBlindApp_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public SplitBlindApp_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements SplitBlindApp_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public SplitBlindApp_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements SplitBlindApp_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public SplitBlindApp_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements SplitBlindApp_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public SplitBlindApp_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements SplitBlindApp_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public SplitBlindApp_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements SplitBlindApp_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public SplitBlindApp_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements SplitBlindApp_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public SplitBlindApp_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends SplitBlindApp_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends SplitBlindApp_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends SplitBlindApp_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends SplitBlindApp_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
      injectMainActivity2(mainActivity);
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(MapBuilder.<String, Boolean>newMapBuilder(6).put(AddExpenseViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, AddExpenseViewModel_HiltModules.KeyModule.provide()).put(CreateGroupViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, CreateGroupViewModel_HiltModules.KeyModule.provide()).put(GroupDetailViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, GroupDetailViewModel_HiltModules.KeyModule.provide()).put(GroupListViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, GroupListViewModel_HiltModules.KeyModule.provide()).put(OnboardingViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, OnboardingViewModel_HiltModules.KeyModule.provide()).put(SettleUpViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, SettleUpViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @CanIgnoreReturnValue
    private MainActivity injectMainActivity2(MainActivity instance) {
      MainActivity_MembersInjector.injectIdentity(instance, singletonCImpl.provideIdentityProvider.get());
      return instance;
    }
  }

  private static final class ViewModelCImpl extends SplitBlindApp_HiltComponents.ViewModelC {
    private final SavedStateHandle savedStateHandle;

    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<AddExpenseViewModel> addExpenseViewModelProvider;

    private Provider<CreateGroupViewModel> createGroupViewModelProvider;

    private Provider<GroupDetailViewModel> groupDetailViewModelProvider;

    private Provider<GroupListViewModel> groupListViewModelProvider;

    private Provider<OnboardingViewModel> onboardingViewModelProvider;

    private Provider<SettleUpViewModel> settleUpViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.savedStateHandle = savedStateHandleParam;
      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.addExpenseViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.createGroupViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.groupDetailViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.groupListViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.onboardingViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.settleUpViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(MapBuilder.<String, javax.inject.Provider<ViewModel>>newMapBuilder(6).put(AddExpenseViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) addExpenseViewModelProvider)).put(CreateGroupViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) createGroupViewModelProvider)).put(GroupDetailViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) groupDetailViewModelProvider)).put(GroupListViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) groupListViewModelProvider)).put(OnboardingViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) onboardingViewModelProvider)).put(SettleUpViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) settleUpViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return Collections.<Class<?>, Object>emptyMap();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.akeshari.splitblind.ui.expenses.AddExpenseViewModel 
          return (T) new AddExpenseViewModel(viewModelCImpl.savedStateHandle, singletonCImpl.groupDao(), singletonCImpl.expenseDao(), singletonCImpl.provideIdentityProvider.get(), singletonCImpl.syncEngineProvider.get());

          case 1: // com.akeshari.splitblind.ui.groups.CreateGroupViewModel 
          return (T) new CreateGroupViewModel(singletonCImpl.groupDao(), singletonCImpl.provideIdentityProvider.get(), singletonCImpl.syncEngineProvider.get());

          case 2: // com.akeshari.splitblind.ui.expenses.GroupDetailViewModel 
          return (T) new GroupDetailViewModel(viewModelCImpl.savedStateHandle, singletonCImpl.groupDao(), singletonCImpl.expenseDao(), singletonCImpl.settlementDao(), singletonCImpl.syncEngineProvider.get(), singletonCImpl.provideIdentityProvider.get());

          case 3: // com.akeshari.splitblind.ui.groups.GroupListViewModel 
          return (T) new GroupListViewModel(singletonCImpl.groupDao(), singletonCImpl.provideIdentityProvider.get(), singletonCImpl.syncEngineProvider.get());

          case 4: // com.akeshari.splitblind.ui.onboarding.OnboardingViewModel 
          return (T) new OnboardingViewModel(singletonCImpl.provideIdentityProvider.get());

          case 5: // com.akeshari.splitblind.ui.settle.SettleUpViewModel 
          return (T) new SettleUpViewModel(viewModelCImpl.savedStateHandle, singletonCImpl.groupDao(), singletonCImpl.settlementDao(), singletonCImpl.syncEngineProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends SplitBlindApp_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends SplitBlindApp_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends SplitBlindApp_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<Identity> provideIdentityProvider;

    private Provider<SplitBlindDatabase> provideDatabaseProvider;

    private Provider<FirebaseDatabase> provideFirebaseDatabaseProvider;

    private Provider<SyncEngine> syncEngineProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    private GroupDao groupDao() {
      return AppModule_ProvideGroupDaoFactory.provideGroupDao(provideDatabaseProvider.get());
    }

    private ExpenseDao expenseDao() {
      return AppModule_ProvideExpenseDaoFactory.provideExpenseDao(provideDatabaseProvider.get());
    }

    private SettlementDao settlementDao() {
      return AppModule_ProvideSettlementDaoFactory.provideSettlementDao(provideDatabaseProvider.get());
    }

    private ProcessedOpDao processedOpDao() {
      return AppModule_ProvideProcessedOpDaoFactory.provideProcessedOpDao(provideDatabaseProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideIdentityProvider = DoubleCheck.provider(new SwitchingProvider<Identity>(singletonCImpl, 0));
      this.provideDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<SplitBlindDatabase>(singletonCImpl, 1));
      this.provideFirebaseDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<FirebaseDatabase>(singletonCImpl, 3));
      this.syncEngineProvider = DoubleCheck.provider(new SwitchingProvider<SyncEngine>(singletonCImpl, 2));
    }

    @Override
    public void injectSplitBlindApp(SplitBlindApp splitBlindApp) {
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return Collections.<Boolean>emptySet();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.akeshari.splitblind.crypto.Identity 
          return (T) AppModule_ProvideIdentityFactory.provideIdentity(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 1: // com.akeshari.splitblind.data.database.SplitBlindDatabase 
          return (T) AppModule_ProvideDatabaseFactory.provideDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 2: // com.akeshari.splitblind.sync.SyncEngine 
          return (T) new SyncEngine(singletonCImpl.provideFirebaseDatabaseProvider.get(), singletonCImpl.groupDao(), singletonCImpl.expenseDao(), singletonCImpl.settlementDao(), singletonCImpl.processedOpDao());

          case 3: // com.google.firebase.database.FirebaseDatabase 
          return (T) FirebaseModule_ProvideFirebaseDatabaseFactory.provideFirebaseDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
