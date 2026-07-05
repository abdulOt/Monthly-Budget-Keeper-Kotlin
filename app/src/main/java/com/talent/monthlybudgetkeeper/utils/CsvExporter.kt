package com.talent.monthlybudgetkeeper.utils

import android.content.Context
import android.net.Uri
import com.talent.monthlybudgetkeeper.data.local.entity.TransactionEntity
import java.io.OutputStreamWriter

object CsvExporter {
    suspend fun exportMonthlyTransactions(
        context: Context,
        uri: Uri,
        transactions: List<TransactionEntity>
    ) {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            OutputStreamWriter(stream).use { writer ->
                writer.appendLine("date,title,type,category,amount,note")
                transactions.forEach { transaction ->
                    writer.appendLine(
                        listOf(
                            transaction.date.toString().csvSafe(),
                            transaction.title.csvSafe(),
                            transaction.type.name.csvSafe(),
                            transaction.category.displayName.csvSafe(),
                            transaction.amount.toString().csvSafe(),
                            transaction.note.csvSafe()
                        ).joinToString(",")
                    )
                }
            }
        }
    }

    private fun String.csvSafe(): String {
        val escaped = replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
