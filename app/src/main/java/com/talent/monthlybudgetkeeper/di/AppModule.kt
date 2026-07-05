package com.talent.monthlybudgetkeeper.di

import android.content.Context
import androidx.room.Room
import com.talent.monthlybudgetkeeper.BuildConfig
import com.talent.monthlybudgetkeeper.data.local.dao.BudgetDao
import com.talent.monthlybudgetkeeper.data.local.dao.FinanceDao
import com.talent.monthlybudgetkeeper.data.local.dao.TransactionDao
import com.talent.monthlybudgetkeeper.data.local.database.BudgetKeeperDatabase
import com.talent.monthlybudgetkeeper.data.local.database.DatabaseMigrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.PropertyConversionMethod
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BudgetKeeperDatabase {
        return Room.databaseBuilder(
            context,
            BudgetKeeperDatabase::class.java,
            "monthly_budget_keeper.db"
        ).addMigrations(
            DatabaseMigrations.MIGRATION_1_2,
            DatabaseMigrations.MIGRATION_2_3,
            DatabaseMigrations.MIGRATION_3_4
        )
            .build()
    }

    @Provides
    fun provideTransactionDao(database: BudgetKeeperDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideBudgetDao(database: BudgetKeeperDatabase): BudgetDao = database.budgetDao()

    @Provides
    fun provideFinanceDao(database: BudgetKeeperDatabase): FinanceDao = database.financeDao()

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = normalizedSupabaseUrl(BuildConfig.SUPABASE_URL),
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY.ifBlank { "placeholder-anon-key" }
        ) {
            install(Auth) {
                host = BuildConfig.SUPABASE_REDIRECT_HOST
                scheme = BuildConfig.SUPABASE_REDIRECT_SCHEME
            }
            install(Postgrest) {
                propertyConversionMethod = PropertyConversionMethod.SERIAL_NAME
            }
        }
    }

    private fun normalizedSupabaseUrl(rawUrl: String): String {
        val url = rawUrl.ifBlank { "https://placeholder.invalid" }
            .trim()
            .trimEnd('/')
        val endpointIndex = listOf("/rest/v1", "/auth/v1", "/storage/v1", "/functions/v1")
            .map(url::indexOf)
            .filter { it >= 0 }
            .minOrNull()

        return if (endpointIndex == null) url else url.substring(0, endpointIndex)
    }
}
