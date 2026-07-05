package com.talent.monthlybudgetkeeper.data.model

import java.time.DayOfWeek

enum class WeekStartOption(val dayOfWeek: DayOfWeek, val label: String) {
    MONDAY(DayOfWeek.MONDAY, "Monday"),
    SATURDAY(DayOfWeek.SATURDAY, "Saturday"),
    SUNDAY(DayOfWeek.SUNDAY, "Sunday")
}
