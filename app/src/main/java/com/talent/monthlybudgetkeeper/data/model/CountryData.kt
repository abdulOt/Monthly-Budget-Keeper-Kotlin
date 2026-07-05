package com.talent.monthlybudgetkeeper.data.model

import java.util.Locale

data class CountryEntry(
    val isoCode: String,
    val name: String,
    val languageTag: String
)

object CountryData {
    val allCountries: List<CountryEntry> by lazy {
        Locale.getISOCountries()
            .mapNotNull { countryCode ->
                val displayName = Locale("", countryCode).getDisplayCountry(Locale.US).trim()
                if (displayName.isBlank()) {
                    null
                } else {
                    CountryEntry(
                        isoCode = countryCode,
                        name = displayName,
                        languageTag = "${defaultLanguageFor(countryCode)}-$countryCode"
                    )
                }
            }
            .distinctBy { it.isoCode }
            .sortedBy { it.name }
    }

    private fun defaultLanguageFor(countryCode: String): String {
        return when (countryCode.uppercase()) {
            "DE", "AT" -> "de"
            "FR", "BE" -> "fr"
            "ES", "MX", "AR", "CO", "CL", "PE", "VE", "UY", "BO", "PY", "EC", "GT", "HN", "NI", "PA", "SV", "CR", "DO", "CU" -> "es"
            "IT" -> "it"
            "PT", "BR" -> "pt"
            "NL" -> "nl"
            "SE" -> "sv"
            "NO" -> "no"
            "DK" -> "da"
            "FI" -> "fi"
            "PL" -> "pl"
            "TR" -> "tr"
            "SA", "AE", "QA", "KW", "OM", "BH", "EG", "JO", "LB", "IQ", "SY", "YE", "DZ", "MA", "TN", "LY" -> "ar"
            "IR", "AF" -> "fa"
            "PK" -> "en"
            "IN" -> "en"
            "CN" -> "zh"
            "JP" -> "ja"
            "KR" -> "ko"
            "RU" -> "ru"
            "UA" -> "uk"
            "CZ" -> "cs"
            "SK" -> "sk"
            "HU" -> "hu"
            "RO", "MD" -> "ro"
            "BG" -> "bg"
            "GR", "CY" -> "el"
            "IL" -> "he"
            "TH" -> "th"
            "VN" -> "vi"
            "ID" -> "id"
            "MY" -> "ms"
            else -> "en"
        }
    }
}
