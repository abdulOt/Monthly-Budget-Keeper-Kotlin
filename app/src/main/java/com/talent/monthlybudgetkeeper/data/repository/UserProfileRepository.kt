package com.talent.monthlybudgetkeeper.data.repository

import com.talent.monthlybudgetkeeper.data.auth.SessionManager
import com.talent.monthlybudgetkeeper.data.model.CloudProfileRow
import com.talent.monthlybudgetkeeper.data.model.CloudTables
import com.talent.monthlybudgetkeeper.data.model.CloudUserSettingsRow
import com.talent.monthlybudgetkeeper.data.model.BudgetCycleType
import com.talent.monthlybudgetkeeper.data.model.CurrencyOption
import com.talent.monthlybudgetkeeper.data.model.RegionOption
import com.talent.monthlybudgetkeeper.data.security.UserOwnershipValidator
import java.time.LocalDate
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonObject

enum class PostLoginDestination {
    SETUP,
    HOME
}

data class UserSetupData(
    val region: RegionOption,
    val currency: CurrencyOption,
    val monthlyIncome: Double,
    val monthlyBudgetTarget: Double,
    val mainFinancialGoal: String,
    val cycleType: BudgetCycleType,
    val cycleStartDate: LocalDate,
    val nextCycleDate: LocalDate,
    val carryForwardRemainingBudget: Boolean,
    val notificationsEnabled: Boolean,
    val privacyModeEnabled: Boolean
)

@Singleton
class UserProfileRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val sessionManager: SessionManager,
    private val settingsRepository: SettingsRepository
) {

    fun getCurrentUserId(): String? = sessionManager.currentUserOrNull()?.id

    suspend fun getUserSettings(userId: String): CloudUserSettingsRow? {
        val raw = supabaseClient.from(CloudTables.USER_SETTINGS).select {
            filter { eq("user_id", userId) }
        }.decodeSingleOrNull<JsonObject>() ?: return null
        return raw.toCloudUserSettingsRow(userId).also { settings ->
            UserOwnershipValidator.requireOwnedByUser(
                expectedUserId = userId,
                actualUserId = settings.userId,
                subject = CloudTables.USER_SETTINGS
            )
        }
    }

    suspend fun ensureUserSettingsRow(userId: String): CloudUserSettingsRow {
        return getUserSettings(userId) ?: createInitialUserSettings(userId)
    }

    suspend fun createInitialUserSettings(userId: String): CloudUserSettingsRow {
        getUserSettings(userId)?.let { existingSettings ->
            upsertIdentityProfile(userId)
            return existingSettings
        }

        val now = Instant.now().toString()
        val today = LocalDate.now()
        val row = CloudUserSettingsRow(
            id = userId,
            userId = userId,
            setupCompleted = false,
            region = RegionOption.PAKISTAN.label,
            currency = CurrencyOption.PKR.code,
            monthlyIncome = 0.0,
            monthlyBudgetTarget = 0.0,
            mainFinancialGoal = "Track spending",
            cycleType = BudgetCycleType.MONTHLY.name,
            cycleStartDate = today.withDayOfMonth(1).toString(),
            nextCycleDate = today.withDayOfMonth(1).plusMonths(1).toString(),
            carryForwardRemainingBudget = false,
            notificationsEnabled = false,
            privacyModeEnabled = false,
            createdAt = now,
            updatedAt = now
        )

        upsertIdentityProfile(userId)
        upsertUserSettings(row)
        return row
    }

    suspend fun saveCompletedUserSetup(
        userId: String,
        setupData: UserSetupData
    ): CloudUserSettingsRow {
        val existingSettings = getUserSettings(userId)
        val now = Instant.now().toString()
        val row = CloudUserSettingsRow(
            id = existingSettings?.id ?: userId,
            userId = userId,
            setupCompleted = true,
            region = setupData.region.label,
            currency = setupData.currency.code,
            monthlyIncome = setupData.monthlyIncome.coerceAtLeast(0.0),
            monthlyBudgetTarget = setupData.monthlyBudgetTarget.coerceAtLeast(0.0),
            mainFinancialGoal = setupData.mainFinancialGoal.trim().ifBlank { "Track spending" },
            cycleType = setupData.cycleType.name,
            cycleStartDate = setupData.cycleStartDate.toString(),
            nextCycleDate = setupData.nextCycleDate.toString(),
            carryForwardRemainingBudget = setupData.carryForwardRemainingBudget,
            notificationsEnabled = setupData.notificationsEnabled,
            privacyModeEnabled = setupData.privacyModeEnabled,
            createdAt = existingSettings?.createdAt ?: now,
            updatedAt = now
        )

        upsertIdentityProfile(userId)
        upsertUserSettings(row)
        return row
    }

    suspend fun syncUserSettingsToLocal(
        settings: CloudUserSettingsRow,
        preserveSetupResetPending: Boolean = true
    ) {
        settingsRepository.syncUserProfileToLocal(
            profile = settings,
            preserveSetupResetPending = preserveSetupResetPending
        )
    }

    suspend fun clearLocalSessionOnly() {
        settingsRepository.bindUser(null)
        settingsRepository.resetUserScopedPreferences()
    }

    suspend fun logout() {
        clearLocalSessionOnly()
    }

    suspend fun resolvePostLoginDestination(userId: String): PostLoginDestination {
        val settings = getUserSettings(userId)
        if (settings == null) {
            settingsRepository.bindUser(userId)
            return PostLoginDestination.SETUP
        }
        syncUserSettingsToLocal(settings)
        return if (settings.setupCompleted) {
            PostLoginDestination.HOME
        } else {
            PostLoginDestination.SETUP
        }
    }

    private suspend fun upsertIdentityProfile(userId: String) {
        val currentUser = sessionManager.currentUserOrNull()
        val currentPreferences = settingsRepository.getCurrentPreferences()
        val existingProfile = getIdentityProfile(userId)
        val fullName = currentUser?.displayName
            ?.takeIf { it.isNotBlank() }
            ?: currentPreferences.profileName
        val now = System.currentTimeMillis()
        supabaseClient.from(CloudTables.PROFILES).upsert(
            CloudProfileRow(
                id = userId,
                userId = userId,
                email = currentUser?.email.orEmpty(),
                fullName = fullName.ifBlank { "My Budget Profile" },
                createdAt = existingProfile?.createdAt ?: now,
                updatedAt = now
            )
        )
    }

    private suspend fun getIdentityProfile(userId: String): CloudProfileRow? {
        return supabaseClient.from(CloudTables.PROFILES).select {
            filter { eq("user_id", userId) }
        }.decodeSingleOrNull<CloudProfileRow>()?.also { profile ->
            UserOwnershipValidator.requireOwnedByUser(
                expectedUserId = userId,
                actualUserId = profile.userId,
                subject = CloudTables.PROFILES
            )
        }
    }

    suspend fun upsertUserSettings(row: CloudUserSettingsRow) {
        runCatching {
            supabaseClient.from(CloudTables.USER_SETTINGS).upsert(row)
        }.recoverCatching { throwable ->
            if (!throwable.isUserSettingsSchemaMismatch()) throw throwable
            supabaseClient.from(CloudTables.USER_SETTINGS).upsert(row.toModernCompactPayload())
        }.recoverCatching { throwable ->
            if (!throwable.isUserSettingsSchemaMismatch()) throw throwable
            supabaseClient.from(CloudTables.USER_SETTINGS).upsert(row.toLegacyPayload())
        }.getOrThrow()
    }

    private fun JsonObject.toCloudUserSettingsRow(userId: String): CloudUserSettingsRow {
        val currentUser = sessionManager.currentUserOrNull()
        val defaultCycleStart = LocalDate.now().withDayOfMonth(1)
        val defaultNextCycleDate = defaultCycleStart.plusMonths(1)
        return CloudUserSettingsRow(
            id = stringValue("id") ?: userId,
            userId = stringValue("user_id") ?: userId,
            setupCompleted = booleanValue("setup_completed") ?: false,
            currency = stringValue("currency") ?: CurrencyOption.PKR.code,
            region = stringValue("region")
                ?: stringValue("region_code")?.toRegionLabel()
                ?: RegionOption.defaultFor(CurrencyOption.PKR).label,
            monthlyIncome = numberValue("monthly_income") ?: 0.0,
            monthlyBudgetTarget = numberValue("monthly_budget_target") ?: 0.0,
            mainFinancialGoal = stringValue("main_financial_goal")
                ?: stringValue("financial_goal")
                ?: "Track spending",
            cycleType = stringValue("cycle_type") ?: BudgetCycleType.MONTHLY.name,
            cycleStartDate = stringValue("cycle_start_date") ?: defaultCycleStart.toString(),
            nextCycleDate = stringValue("next_cycle_date") ?: defaultNextCycleDate.toString(),
            carryForwardRemainingBudget = booleanValue("carry_forward_remaining_budget") ?: false,
            notificationsEnabled = booleanValue("notifications_enabled") ?: false,
            privacyModeEnabled = booleanValue("privacy_mode_enabled") ?: false,
            createdAt = stringValue("created_at")
                ?: longValue("created_at")?.toIsoTimestamp()
                ?: Instant.now().toString(),
            updatedAt = stringValue("updated_at")
                ?: longValue("updated_at")?.toIsoTimestamp()
                ?: Instant.now().toString()
        )
    }

    private fun CloudUserSettingsRow.toModernCompactPayload() = buildJsonObject {
        put("id", id)
        put("user_id", userId)
        put("setup_completed", setupCompleted)
        put("currency", currency)
        put("region", region)
        put("monthly_income", monthlyIncome)
        put("monthly_budget_target", monthlyBudgetTarget)
        put("main_financial_goal", mainFinancialGoal)
        put("notifications_enabled", notificationsEnabled)
        put("privacy_mode_enabled", privacyModeEnabled)
        put("created_at", createdAt)
        put("updated_at", updatedAt)
    }

    private fun CloudUserSettingsRow.toLegacyPayload() = buildJsonObject {
        put("id", id)
        put("user_id", userId)
        put("setup_completed", setupCompleted)
        put("currency", currency)
        put("region_code", region.toRegionCode())
        put("monthly_income", monthlyIncome)
        put("monthly_budget_target", monthlyBudgetTarget)
        put("financial_goal", mainFinancialGoal)
        put("notifications_enabled", notificationsEnabled)
        put("privacy_mode_enabled", privacyModeEnabled)
        put("created_at", createdAt)
        put("updated_at", updatedAt)
    }

    private fun Throwable.isUserSettingsSchemaMismatch(): Boolean {
        val message = message.orEmpty().lowercase()
        return message.contains("user_settings") && (
            message.contains("column") ||
                message.contains("schema cache") ||
                message.contains("could not find") ||
                message.contains("does not exist")
            )
    }

    private fun JsonObject.stringValue(key: String): String? {
        return this[key]?.jsonPrimitive?.contentOrNull
    }

    private fun JsonObject.booleanValue(key: String): Boolean? {
        return this[key]?.jsonPrimitive?.booleanOrNull
    }

    private fun JsonObject.numberValue(key: String): Double? {
        val primitive = this[key]?.jsonPrimitive ?: return null
        return primitive.doubleOrNull ?: primitive.contentOrNull?.toDoubleOrNull()
    }

    private fun JsonObject.longValue(key: String): Long? {
        val primitive = this[key]?.jsonPrimitive ?: return null
        return primitive.longOrNull
            ?: primitive.intOrNull?.toLong()
            ?: primitive.contentOrNull?.toLongOrNull()
    }

    private fun String.toRegionCode(): String {
        return when (trim().lowercase()) {
            "pakistan" -> "PAKISTAN"
            "germany" -> "GERMANY"
            "united states" -> "UNITED_STATES"
            "united kingdom" -> "UNITED_KINGDOM"
            else -> uppercase().replace(' ', '_')
        }
    }

    private fun String.toRegionLabel(): String {
        return when (trim().uppercase()) {
            "PAKISTAN" -> "Pakistan"
            "GERMANY" -> "Germany"
            "UNITED_STATES" -> "United States"
            "UNITED_KINGDOM" -> "United Kingdom"
            else -> lowercase().split('_').joinToString(" ") { token ->
                token.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }
    }

    private fun Long.toIsoTimestamp(): String {
        return if (this > 1_000_000_000_000L) {
            Instant.ofEpochMilli(this).toString()
        } else {
            Instant.ofEpochSecond(this).toString()
        }
    }
}
