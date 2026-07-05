package com.talent.monthlybudgetkeeper.data.model

enum class TransactionCategory(
    val displayName: String,
    val type: TransactionType
) {
    FOOD("Food", TransactionType.EXPENSE),
    TRANSPORT("Transport", TransactionType.EXPENSE),
    BILLS("Bills", TransactionType.EXPENSE),
    SHOPPING("Shopping", TransactionType.EXPENSE),
    HEALTH("Health", TransactionType.EXPENSE),
    EDUCATION("Education", TransactionType.EXPENSE),
    ENTERTAINMENT("Entertainment", TransactionType.EXPENSE),
    FAMILY("Family", TransactionType.EXPENSE),
    RENT("Rent", TransactionType.EXPENSE),
    OTHER_EXPENSE("Other", TransactionType.EXPENSE),
    SALARY("Salary", TransactionType.INCOME),
    FREELANCE("Freelance", TransactionType.INCOME),
    BUSINESS("Business", TransactionType.INCOME),
    GIFT("Gift", TransactionType.INCOME),
    OTHER_INCOME("Other", TransactionType.INCOME);

    companion object {
        fun forType(type: TransactionType): List<TransactionCategory> {
            return entries.filter { it.type == type }
        }

        fun defaultFor(type: TransactionType): TransactionCategory {
            return if (type == TransactionType.INCOME) SALARY else FOOD
        }
    }
}
