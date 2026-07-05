package com.talent.monthlybudgetkeeper.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class SecurePinStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val activeUserId = MutableStateFlow<String?>(null)
    private val _pinHashFlow = MutableStateFlow<String?>(null)
    val pinHashFlow: StateFlow<String?> = _pinHashFlow.asStateFlow()

    suspend fun bindUser(userId: String?) {
        activeUserId.value = userId
        _pinHashFlow.value = userId?.let(::readPinHashForUser)
    }

    fun currentPinHash(): String? = _pinHashFlow.value

    fun currentPinHashForUser(userId: String): String? = readPinHashForUser(userId)

    fun savePinHash(pinHash: String?) {
        val userId = activeUserId.value
        if (userId.isNullOrBlank()) {
            _pinHashFlow.value = null
            return
        }
        savePinHashForUser(userId, pinHash)
    }

    fun savePinHashForUser(userId: String, pinHash: String?) {
        val pinKey = pinKeyForUser(userId)
        if (pinHash.isNullOrBlank()) {
            sharedPreferences.edit().remove(pinKey).apply()
            if (activeUserId.value == userId) {
                _pinHashFlow.value = null
            }
            return
        }

        val encrypted = encrypt(pinHash)
        sharedPreferences.edit().putString(pinKey, encrypted).apply()
        if (activeUserId.value == userId) {
            _pinHashFlow.value = pinHash
        }
    }

    fun clearLegacyGlobalPin() {
        sharedPreferences.edit().remove(LEGACY_KEY_PIN_HASH).apply()
        if (activeUserId.value == null) {
            _pinHashFlow.value = null
        }
    }

    fun clearAllPins() {
        sharedPreferences.edit().clear().apply()
        _pinHashFlow.value = null
    }

    private fun readPinHashForUser(userId: String): String? {
        val stored = sharedPreferences.getString(pinKeyForUser(userId), null) ?: return null
        return runCatching { decrypt(stored) }.getOrNull()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val encryptedBytes = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val payload = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        return "$iv:$payload"
    }

    private fun decrypt(value: String): String {
        val (ivEncoded, payloadEncoded) = value.split(':', limit = 2)
        val iv = Base64.decode(ivEncoded, Base64.NO_WRAP)
        val payload = Base64.decode(payloadEncoded, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        )
        return String(cipher.doFinal(payload), StandardCharsets.UTF_8)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) return existingKey

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEY_STORE
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return keyGenerator.generateKey()
    }

    private fun pinKeyForUser(userId: String): String = "${USER_PIN_PREFIX}$userId"

    private companion object {
        const val PREFS_NAME = "monthly_budget_keeper_secure"
        const val LEGACY_KEY_PIN_HASH = "encrypted_pin_hash"
        const val USER_PIN_PREFIX = "encrypted_pin_hash_"
        const val KEY_ALIAS = "monthly_budget_keeper_pin_key"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
    }
}
