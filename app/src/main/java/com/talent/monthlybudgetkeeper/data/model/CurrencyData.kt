package com.talent.monthlybudgetkeeper.data.model

import java.util.Currency
import java.util.Locale

data class CurrencyEntry(
    val code: String,
    val displayName: String,
    val symbol: String
)

object CurrencyData {
    val allCurrencies: List<CurrencyEntry> by lazy {
        Currency.getAvailableCurrencies()
            .map { currency ->
                CurrencyEntry(
                    code = currency.currencyCode,
                    displayName = currency.getDisplayName(Locale.US),
                    symbol = runCatching { currency.getSymbol(Locale.US) }.getOrDefault(currency.currencyCode)
                )
            }
            .sortedBy { it.code }
    }
}
