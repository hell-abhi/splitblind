package com.akeshari.splitblind.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val expenseId: String,
    val groupId: String,
    val description: String,
    val amountCents: Long,
    val currency: String = "INR",
    val paidBy: String,
    val splitAmong: String,
    val createdAt: Long,
    val isDeleted: Boolean = false,
    val hlcTimestamp: Long,
    val tag: String? = null,
    val paidByMap: String? = null,       // JSON: {"memberId": cents}
    val splitMode: String? = null,       // "equal", "amount", "percentage", "ratio", "items"
    val splitDetails: String? = null,    // JSON: {"memberId": cents}
    val notes: String? = null,
    val recurringFrequency: String? = null, // "weekly", "monthly", "yearly"
    val splitItems: String? = null,       // JSON array of item-wise split data
    val convertedAmountCents: Long? = null,
    val conversionRate: Double? = null,
    val convertedCurrency: String? = null
)
