package com.akeshari.splitblind.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.akeshari.splitblind.ui.expenses.AddExpenseScreen
import com.akeshari.splitblind.ui.expenses.GroupDetailScreen
import com.akeshari.splitblind.ui.groups.CreateGroupScreen
import com.akeshari.splitblind.ui.groups.GroupListScreen
import com.akeshari.splitblind.ui.onboarding.OnboardingScreen
import com.akeshari.splitblind.ui.security.SecurityScreen
import com.akeshari.splitblind.ui.settle.SettleUpScreen
import com.akeshari.splitblind.ui.sync.SyncDeviceScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val GROUPS = "groups"
    const val CREATE_GROUP = "create_group"
    const val GROUP_DETAIL = "group_detail/{groupId}"
    const val ADD_EXPENSE = "add_expense/{groupId}"
    const val SETTLE = "settle/{groupId}/{from}/{to}/{amountCents}"
    const val SECURITY = "security"
    const val SYNC_GENERATE = "sync_generate"
    const val SYNC_RESTORE = "sync_restore"

    fun groupDetail(groupId: String) = "group_detail/$groupId"
    fun addExpense(groupId: String) = "add_expense/$groupId"
    fun settle(groupId: String, from: String, to: String, amountCents: Long) =
        "settle/$groupId/$from/$to/$amountCents"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    isOnboarded: Boolean,
    joinGroupId: String? = null,
    joinGroupKey: String? = null,
    joinGroupName: String? = null
) {
    val startDestination = if (isOnboarded) Routes.GROUPS else Routes.ONBOARDING

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Routes.GROUPS) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
                onRestore = {
                    navController.navigate(Routes.SYNC_RESTORE)
                }
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
                onSecurityClick = {
                    navController.navigate(Routes.SECURITY)
                },
                onSyncClick = {
                    navController.navigate(Routes.SYNC_GENERATE)
                },
                joinGroupId = joinGroupId,
                joinGroupKey = joinGroupKey,
                joinGroupName = joinGroupName
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
                onSettle = { groupId, from, to, amountCents ->
                    navController.navigate(Routes.settle(groupId, from, to, amountCents))
                }
            )
        }

        composable(
            Routes.ADD_EXPENSE,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
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
                    navController.navigate(Routes.GROUPS) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
