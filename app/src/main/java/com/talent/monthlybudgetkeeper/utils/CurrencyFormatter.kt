package com.talent.monthlybudgetkeeper.utils

import com.talent.monthlybudgetkeeper.data.model.CurrencyOption
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object CurrencyFormatter {
    fun format(amount: Double, currency: CurrencyOption): String {
        return runCatching {
            val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
                this.currency = Currency.getInstance(currency.code)
                minimumFractionDigits = 0
                maximumFractionDigits = 2
            }
            formatter.format(amount)
        }.getOrElse {
            "${currency.symbol}${"%,.2f".format(Locale.US, amount)}"
        }
    }
}
