package com.talent.monthlybudgetkeeper.data.model

data class CurrencyOption(
    val code: String,
    val symbol: String,
    val displayName: String
) {
    val selectorLabel: String
        get() = "$code - $displayName"

    companion object {
        val entries: List<CurrencyOption> by lazy {
            CurrencyData.allCurrencies.map {
                CurrencyOption(
                    code = it.code,
                    symbol = it.symbol,
                    displayName = it.displayName
                )
            }
        }

        val PKR: CurrencyOption by lazy { fromCodeOrDefault("PKR", entries.first()) }
        val USD: CurrencyOption by lazy { fromCodeOrDefault("USD", PKR) }
        val EUR: CurrencyOption by lazy { fromCodeOrDefault("EUR", USD) }
        val GBP: CurrencyOption by lazy { fromCodeOrDefault("GBP", USD) }
        val AED: CurrencyOption by lazy { fromCodeOrDefault("AED", USD) }

        fun fromCodeOrDefault(
            value: String?,
            default: CurrencyOption = PKR
        ): CurrencyOption {
            return entries.firstOrNull { option ->
                option.code.equals(value, ignoreCase = true) ||
                    option.displayName.equals(value, ignoreCase = true)
            } ?: default
        }
    }
}
