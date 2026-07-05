package com.talent.monthlybudgetkeeper.data.model

data class RegionOption(
    val isoCode: String,
    val languageTag: String,
    val label: String
) {
    companion object {
        val entries: List<RegionOption> by lazy {
            CountryData.allCountries.map {
                RegionOption(
                    isoCode = it.isoCode,
                    languageTag = it.languageTag,
                    label = it.name
                )
            }
        }

        val PAKISTAN: RegionOption by lazy { fromNameOrDefault("Pakistan", entries.first()) }
        val UNITED_STATES: RegionOption by lazy { fromNameOrDefault("United States", PAKISTAN) }
        val UNITED_KINGDOM: RegionOption by lazy { fromNameOrDefault("United Kingdom", UNITED_STATES) }
        val GERMANY: RegionOption by lazy { fromNameOrDefault("Germany", UNITED_STATES) }

        fun defaultFor(currency: CurrencyOption): RegionOption {
            return when (currency.code.uppercase()) {
                "PKR" -> PAKISTAN
                "EUR" -> GERMANY
                "GBP" -> UNITED_KINGDOM
                "AED" -> fromNameOrDefault("United Arab Emirates", UNITED_STATES)
                else -> UNITED_STATES
            }
        }

        fun fromNameOrDefault(
            value: String?,
            default: RegionOption
        ): RegionOption {
            return entries.firstOrNull { option ->
                option.label.equals(value, ignoreCase = true) ||
                    option.isoCode.equals(value, ignoreCase = true) ||
                    option.languageTag.equals(value, ignoreCase = true) ||
                    option.label.replace(" ", "_").equals(value, ignoreCase = true)
            } ?: default
        }

        fun fromNameOrDefault(value: String?, currency: CurrencyOption): RegionOption {
            return fromNameOrDefault(value, defaultFor(currency))
        }
    }
}
