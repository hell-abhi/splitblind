package com.akeshari.splitblind.ui.navigation

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val GROUPS = "groups"
    const val PROFILE = "profile"
    const val CREATE_GROUP = "create_group"
    const val GROUP_DETAIL = "group_detail/{groupId}"
    const val ADD_EXPENSE = "add_expense/{groupId}?editExpenseId={editExpenseId}"
    const val SETTLE = "settle/{groupId}/{from}/{to}/{amountCents}"
    const val SECURITY = "security"
    const val SETUP_PASSPHRASE = "setup_passphrase"
    const val RECOVER = "recover"
    const val SYNC_GENERATE = "sync_generate"
    const val SYNC_RESTORE = "sync_restore"
    const val ANALYTICS = "analytics"

    fun groupDetail(groupId: String) = "group_detail/$groupId"
    fun addExpense(groupId: String) = "add_expense/$groupId"
    fun editExpense(groupId: String, expenseId: String) = "add_expense/$groupId?editExpenseId=$expenseId"
    fun settle(groupId: String, from: String, to: String, amountCents: Long) =
        "settle/$groupId/$from/$to/$amountCents"
}
