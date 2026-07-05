# Monthly Budget Keeper

## 1. Project overview
- Offline-first Android finance app built with Kotlin, Jetpack Compose, MVVM, Room, Hilt, Navigation Compose, StateFlow, and Material 3.
- Tracks income, expenses, monthly budgets, category limits, analytics, transaction history, onboarding state, and local preferences.
- Uses Room for financial data and DataStore Preferences for onboarding/settings/profile data.

## 2. Full folder/package structure
```text
Monthly Budget Keeper/
├── build.gradle.kts
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/gradle-wrapper.properties
├── gradlew
├── gradlew.bat
├── settings.gradle.kts
├── PROJECT_OVERVIEW.md
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/talent/monthlybudgetkeeper/
        │   ├── AppState.kt
        │   ├── BudgetKeeperApp.kt
        │   ├── MainActivity.kt
        │   ├── data/
        │   │   ├── local/
        │   │   │   ├── Converters.kt
        │   │   │   ├── dao/BudgetDao.kt
        │   │   │   ├── dao/TransactionDao.kt
        │   │   │   ├── database/BudgetKeeperDatabase.kt
        │   │   │   ├── entity/CategoryBudgetEntity.kt
        │   │   │   ├── entity/MonthlyBudgetEntity.kt
        │   │   │   └── entity/TransactionEntity.kt
        │   │   ├── model/
        │   │   │   ├── AppPreferences.kt
        │   │   │   ├── BudgetOverview.kt
        │   │   │   ├── CategorySpend.kt
        │   │   │   ├── CurrencyOption.kt
        │   │   │   ├── MonthlyTrend.kt
        │   │   │   ├── TransactionCategory.kt
        │   │   │   ├── TransactionFilter.kt
        │   │   │   ├── TransactionSortOption.kt
        │   │   │   ├── TransactionType.kt
        │   │   │   └── WeekStartOption.kt
        │   │   └── repository/
        │   │       ├── BudgetRepository.kt
        │   │       ├── SettingsRepository.kt
        │   │       └── TransactionRepository.kt
        │   ├── di/AppModule.kt
        │   ├── ui/
        │   │   ├── components/
        │   │   │   ├── BudgetProgressCard.kt
        │   │   │   ├── CategoryVisuals.kt
        │   │   │   ├── DashboardAmountCard.kt
        │   │   │   ├── DatePickerField.kt
        │   │   │   ├── EmptyStateCard.kt
        │   │   │   ├── FinanceCharts.kt
        │   │   │   ├── MonthSelector.kt
        │   │   │   ├── QuickActionButton.kt
        │   │   │   └── TransactionItemRow.kt
        │   │   ├── navigation/
        │   │   │   ├── AppDestination.kt
        │   │   │   └── AppNavGraph.kt
        │   │   ├── screens/
        │   │   │   ├── budget/BudgetScreen.kt
        │   │   │   ├── history/HistoryScreen.kt
        │   │   │   ├── home/HomeScreen.kt
        │   │   │   ├── launch/LaunchScreen.kt
        │   │   │   ├── onboarding/OnboardingScreen.kt
        │   │   │   ├── reports/ReportsScreen.kt
        │   │   │   ├── settings/SettingsScreen.kt
        │   │   │   └── transaction/
        │   │   │       ├── TransactionDetailScreen.kt
        │   │   │       └── TransactionFormScreen.kt
        │   │   └── theme/
        │   │       ├── Color.kt
        │   │       ├── Theme.kt
        │   │       └── Type.kt
        │   ├── utils/
        │   │   ├── CsvExporter.kt
        │   │   ├── CurrencyFormatter.kt
        │   │   ├── DateUtils.kt
        │   │   └── TransactionValidators.kt
        │   └── viewmodel/
        │       ├── BudgetViewModel.kt
        │       ├── HistoryViewModel.kt
        │       ├── HomeViewModel.kt
        │       ├── LaunchViewModel.kt
        │       ├── OnboardingViewModel.kt
        │       ├── ReportsViewModel.kt
        │       ├── SettingsViewModel.kt
        │       ├── TransactionDetailViewModel.kt
        │       └── TransactionFormViewModel.kt
        └── res/values/
            ├── strings.xml
            └── themes.xml
```

## 3. Gradle files and dependency setup
- Root: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`
- Version catalog: `gradle/libs.versions.toml`
- App module: `app/build.gradle.kts`
- Wrapper config: `gradle/wrapper/gradle-wrapper.properties`, `gradlew`, `gradlew.bat`

## 4. Manifest and application class
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/talent/monthlybudgetkeeper/BudgetKeeperApp.kt`
- `app/src/main/java/com/talent/monthlybudgetkeeper/MainActivity.kt`
- `app/src/main/java/com/talent/monthlybudgetkeeper/AppState.kt`

## 5. Room entities
- `TransactionEntity.kt`
- `MonthlyBudgetEntity.kt`
- `CategoryBudgetEntity.kt`

## 6. DAO interfaces
- `TransactionDao.kt`
- `BudgetDao.kt`

## 7. Database class
- `BudgetKeeperDatabase.kt`
- `Converters.kt`

## 8. Repository classes
- `TransactionRepository.kt`
- `BudgetRepository.kt`
- `SettingsRepository.kt`

## 9. DataStore / settings persistence
- `SettingsRepository.kt`
- `AppPreferences.kt`
- `CurrencyOption.kt`
- `WeekStartOption.kt`

## 10. Navigation setup
- `AppDestination.kt`
- `AppNavGraph.kt`

## 11. ViewModels
- `LaunchViewModel.kt`
- `OnboardingViewModel.kt`
- `HomeViewModel.kt`
- `HistoryViewModel.kt`
- `BudgetViewModel.kt`
- `ReportsViewModel.kt`
- `SettingsViewModel.kt`
- `TransactionFormViewModel.kt`
- `TransactionDetailViewModel.kt`

## 12. Reusable UI components
- `CategoryVisuals.kt`
- `DashboardAmountCard.kt`
- `QuickActionButton.kt`
- `TransactionItemRow.kt`
- `EmptyStateCard.kt`
- `MonthSelector.kt`
- `BudgetProgressCard.kt`
- `FinanceCharts.kt`
- `DatePickerField.kt`

## 13. Theme files
- `ui/theme/Color.kt`
- `ui/theme/Type.kt`
- `ui/theme/Theme.kt`
- `res/values/themes.xml`

## 14. Screens one by one
- Launch: `ui/screens/launch/LaunchScreen.kt`
- Onboarding: `ui/screens/onboarding/OnboardingScreen.kt`
- Home/dashboard: `ui/screens/home/HomeScreen.kt`
- Add/edit transaction: `ui/screens/transaction/TransactionFormScreen.kt`
- Transaction detail: `ui/screens/transaction/TransactionDetailScreen.kt`
- History: `ui/screens/history/HistoryScreen.kt`
- Budget management: `ui/screens/budget/BudgetScreen.kt`
- Reports: `ui/screens/reports/ReportsScreen.kt`
- Settings: `ui/screens/settings/SettingsScreen.kt`

## 15. Utility/helper files
- `CurrencyFormatter.kt`
- `DateUtils.kt`
- `TransactionValidators.kt`
- `CsvExporter.kt`

## 16. CSV export implementation
- CSV writer: `utils/CsvExporter.kt`
- Export trigger and SAF document flow: `ui/screens/reports/ReportsScreen.kt`

## 17. Final review for missing imports / compile errors / dependency issues
- Checked and corrected root activity to use the current `AppNavGraph` signature.
- Replaced the DataStore reset call with a direct `edit { clear() }` block.
- Corrected Compose `supportingText` usage in the transaction form.
- Corrected navigation bar padding import to Compose foundation layout.
- Reviewed route names so navigation destinations and `SavedStateHandle` arguments align.
- Verified Room entities, DAO signatures, and repository methods match the viewmodel usage.
- Verified the app uses real implementations only; there are no TODO/FIXME placeholders.

## 18. Final run steps in Android Studio
1. Open the folder in Android Studio.
2. Let Android Studio download the Android 16 / API 36 SDK and build tools if prompted.
3. Use JDK 17 for the project.
4. Sync Gradle.
5. Run the `app` configuration on an emulator or device with API 26+.

## Notes
- The workspace includes wrapper scripts and wrapper properties. The generated `gradle-wrapper.jar` binary is not present because it was not available to generate inside this environment.
- The dependency choices are aligned to current stable Android 16 / AGP 9.1.0 era tooling.
