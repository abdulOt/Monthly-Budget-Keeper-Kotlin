package com.talent.monthlybudgetkeeper.utils

import android.content.Context
import android.net.Uri
import com.talent.monthlybudgetkeeper.data.model.BackupPayload
import java.io.OutputStreamWriter
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

object JsonBackupManager {
    private const val MAX_BACKUP_SIZE_BYTES = 15 * 1024 * 1024

    private val serializer = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    suspend fun exportBackup(
        context: Context,
        uri: Uri,
        payload: BackupPayload
    ) {
        validatePayload(payload)
        val content = serializer.encodeToString(BackupPayload.serializer(), payload)
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            OutputStreamWriter(stream).use { writer ->
                writer.write(content)
            }
        } ?: error("Unable to open backup destination.")
    }

    suspend fun importBackup(
        context: Context,
        uri: Uri
    ): BackupPayload {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: error("Unable to open backup file.")

        require(bytes.isNotEmpty()) { "Backup file is empty." }
        require(bytes.size <= MAX_BACKUP_SIZE_BYTES) { "Backup file is too large." }

        val payload = try {
            serializer.decodeFromString(
                BackupPayload.serializer(),
                bytes.toString(Charsets.UTF_8)
            )
        } catch (error: SerializationException) {
            error("Backup file format is invalid.")
        }

        validatePayload(payload)
        return payload
    }

    private fun validatePayload(payload: BackupPayload) {
        require(payload.format == BackupPayload.FORMAT) { "Backup format is not supported." }
        require(payload.version in 1..BackupPayload.VERSION) { "Backup version is not supported." }
        require(payload.preferences.id.isNotBlank()) { "Backup preferences are invalid." }
        require(payload.transactions.size <= 50_000) { "Backup contains too many transactions." }
        require(payload.accounts.size <= 5_000) { "Backup contains too many accounts." }
    }
}
