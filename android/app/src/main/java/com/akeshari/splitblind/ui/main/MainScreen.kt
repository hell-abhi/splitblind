package com.akeshari.splitblind.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.akeshari.splitblind.crypto.Identity
import com.akeshari.splitblind.ui.analytics.AnalyticsScreen
import com.akeshari.splitblind.ui.expenses.AddExpenseScreen
import com.akeshari.splitblind.ui.expenses.GroupDetailScreen
import com.akeshari.splitblind.ui.groups.CreateGroupScreen
import com.akeshari.splitblind.ui.groups.GroupListScreen
import com.akeshari.splitblind.ui.home.HomeScreen
import com.akeshari.splitblind.ui.navigation.Routes
import com.akeshari.splitblind.ui.onboarding.OnboardingScreen
import com.akeshari.splitblind.ui.profile.ProfileScreen
import com.akeshari.splitblind.ui.recovery.RecoverScreen
import com.akeshari.splitblind.ui.recovery.SetupPassphraseScreen
import com.akeshari.splitblind.ui.security.SecurityScreen
import com.akeshari.splitblind.ui.settle.SettleUpScreen
import com.akeshari.splitblind.ui.sync.SyncDeviceScreen

data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

data class DeepLinkData(
    val groupId: String,
    val groupKey: String,
    val groupName: String
)

@Composable
fun MainScreen(
    isOnboarded: Boolean,
    deepLinkData: DeepLinkData?,
    identity: Identity
) {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    val tabs = listOf(
        BottomTab(Routes.HOME, "Home", Icons.Default.Home),
        BottomTab(Routes.GROUPS, "Groups", Icons.Default.Group),
        BottomTab(Routes.ANALYTICS, "Analytics", Icons.Default.BarChart),
        BottomTab(Routes.PROFILE, "Profile", Icons.Default.Person)
    )

    val tabRoutes = tabs.map { it.route }
    val showBottomBar = currentRoute in tabRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isOnboarded) Routes.HOME else Routes.ONBOARDING,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(Routes.SETUP_PASSPHRASE) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    },
                    onRestore = {
                        navController.navigate(Routes.SYNC_RESTORE)
                    },
                    onRecover = {
                        navController.navigate(Routes.RECOVER)
                    }
                )
            }

            composable(Routes.HOME) {
                HomeScreen(
                    onPersonalTrackerClick = { groupId ->
                        navController.navigate(Routes.groupDetail(groupId))
                    },
                    onSyncClick = { navController.navigate(Routes.SYNC_GENERATE) },
                    onSecurityClick = { navController.navigate(Routes.SECURITY) }
                )
            }

            composable(Routes.GROUPS) {
                GroupListScreen(
                    onGroupClick = { groupId ->
                        navController.navigate(Routes.groupDetail(groupId))
                    },
                    onCreateGroup = {
                        navController.navigate(Routes.CREATE_GROUP)
                    },
                    onSyncClick = {
                        navController.navigate(Routes.SYNC_GENERATE)
                    },
                    onSecurityClick = {
                        navController.navigate(Routes.SECURITY)
                    },
                    joinGroupId = deepLinkData?.groupId,
                    joinGroupKey = deepLinkData?.groupKey,
                    joinGroupName = deepLinkData?.groupName
                )
            }

            composable(Routes.ANALYTICS) {
                AnalyticsScreen()
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    identity = identity,
                    onSyncClick = {
                        navController.navigate(Routes.SYNC_GENERATE)
                    },
                    onSecurityClick = {
                        navController.navigate(Routes.SECURITY)
                    },
                    onSetupPassphrase = {
                        navController.navigate(Routes.SETUP_PASSPHRASE)
                    }
                )
            }

            composable(Routes.CREATE_GROUP) {
                CreateGroupScreen(
                    onBack = { navController.popBackStack() },
                    onGroupCreated = { groupId ->
                        navController.navigate(Routes.groupDetail(groupId)) {
                            popUpTo(Routes.GROUPS)
                        }
                    }
                )
            }

            composable(
                Routes.GROUP_DETAIL,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) {
                GroupDetailScreen(
                    onBack = { navController.popBackStack() },
                    onAddExpense = { groupId ->
                        navController.navigate(Routes.addExpense(groupId))
                    },
                    onEditExpense = { groupId, expenseId ->
                        navController.navigate(Routes.editExpense(groupId, expenseId))
                    },
                    onSettle = { groupId, from, to, amountCents ->
                        navController.navigate(Routes.settle(groupId, from, to, amountCents))
                    }
                )
            }

            composable(
                Routes.ADD_EXPENSE,
                arguments = listOf(
                    navArgument("groupId") { type = NavType.StringType },
                    navArgument("editExpenseId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {
                AddExpenseScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                Routes.SETTLE,
                arguments = listOf(
                    navArgument("groupId") { type = NavType.StringType },
                    navArgument("from") { type = NavType.StringType },
                    navArgument("to") { type = NavType.StringType },
                    navArgument("amountCents") { type = NavType.LongType }
                )
            ) {
                SettleUpScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.SECURITY) {
                SecurityScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.SETUP_PASSPHRASE) {
                SetupPassphraseScreen(
                    onComplete = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.SETUP_PASSPHRASE) { inclusive = true }
                        }
                    },
                    onSkip = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.SETUP_PASSPHRASE) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.RECOVER) {
                RecoverScreen(
                    onBack = { navController.popBackStack() },
                    onRecoverComplete = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.SYNC_GENERATE) {
                SyncDeviceScreen(
                    mode = "generate",
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.SYNC_RESTORE) {
                SyncDeviceScreen(
                    mode = "restore",
                    onBack = { navController.popBackStack() },
                    onRestoreComplete = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
