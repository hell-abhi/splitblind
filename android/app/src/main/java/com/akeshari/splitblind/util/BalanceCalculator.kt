package com.akeshari.splitblind.util

import com.akeshari.splitblind.data.database.entity.ExpenseEntity
import com.akeshari.splitblind.data.database.entity.SettlementEntity
import kotlinx.serialization.json.Json

data class Debt(val from: String, val to: String, val amountCents: Long)

private val json = Json { ignoreUnknownKeys = true }

object BalanceCalculator {
    fun computeNetBalances(expenses: List<ExpenseEntity>, settlements: List<SettlementEntity>): Map<String, Long> {
        val balances = mutableMapOf<String, Long>()
        for (expense in expenses) {
            // Use converted amount (base currency) if available
            val effectiveAmountCents = expense.convertedAmountCents ?: expense.amountCents

            // --- Credit payers ---
            val paidByMap: Map<String, Long>? = expense.paidByMap?.let {
                try { json.decodeFromString<Map<String, Long>>(it) } catch (_: Exception) { null }
            }
            if (paidByMap != null && paidByMap.isNotEmpty()) {
                if (expense.convertedAmountCents != null && expense.conversionRate != null) {
                    // Scale multi-payer amounts by conversion rate
                    for ((memberId, amount) in paidByMap) {
                        val convertedPayer = (amount * expense.conversionRate).toLong()
                        balances[memberId] = (balances[memberId] ?: 0) + convertedPayer
                    }
                } else {
                    for ((memberId, amount) in paidByMap) {
                        balances[memberId] = (balances[memberId] ?: 0) + amount
                    }
                }
            } else {
                balances[expense.paidBy] = (balances[expense.paidBy] ?: 0) + effectiveAmountCents
            }

            // --- Debit split members ---
            val splitDetailsMap: Map<String, Long>? = expense.splitDetails?.let {
                try { json.decodeFromString<Map<String, Long>>(it) } catch (_: Exception) { null }
            }
            if (splitDetailsMap != null && splitDetailsMap.isNotEmpty()) {
                if (expense.convertedAmountCents != null && expense.conversionRate != null) {
                    // Scale split details by conversion rate
                    for ((memberId, amount) in splitDetailsMap) {
                        val convertedSplit = (amount * expense.conversionRate).toLong()
                        balances[memberId] = (balances[memberId] ?: 0) - convertedSplit
                    }
                } else {
                    for ((memberId, amount) in splitDetailsMap) {
                        balances[memberId] = (balances[memberId] ?: 0) - amount
                    }
                }
            } else {
                // Fall back to equal split
                val splitMembers: List<String> = json.decodeFromString(expense.splitAmong)
                val share = effectiveAmountCents / splitMembers.size
                val remainder = effectiveAmountCents % splitMembers.size
                for ((index, member) in splitMembers.withIndex()) {
                    val memberShare = share + if (index < remainder) 1 else 0
                    balances[member] = (balances[member] ?: 0) - memberShare
                }
            }
        }
        for (settlement in settlements) {
            balances[settlement.fromMember] = (balances[settlement.fromMember] ?: 0) + settlement.amountCents
            balances[settlement.toMember] = (balances[settlement.toMember] ?: 0) - settlement.amountCents
        }
        return balances
    }

    fun simplifyDebts(netBalances: Map<String, Long>): List<Debt> {
        val creditors = mutableListOf<Pair<String, Long>>()
        val debtors = mutableListOf<Pair<String, Long>>()
        for ((member, balance) in netBalances) {
            when { balance > 0 -> creditors.add(member to balance); balance < 0 -> debtors.add(member to -balance) }
        }
        creditors.sortByDescending { it.second }; debtors.sortByDescending { it.second }
        val debts = mutableListOf<Debt>()
        var i = 0; var j = 0
        while (i < creditors.size && j < debtors.size) {
            val (creditor, credit) = creditors[i]; val (debtor, debt) = debtors[j]
            val amount = minOf(credit, debt)
            if (amount > 0) debts.add(Debt(from = debtor, to = creditor, amountCents = amount))
            creditors[i] = creditor to (credit - amount); debtors[j] = debtor to (debt - amount)
            if (creditors[i].second == 0L) i++; if (debtors[j].second == 0L) j++
        }
        return debts
    }
}
