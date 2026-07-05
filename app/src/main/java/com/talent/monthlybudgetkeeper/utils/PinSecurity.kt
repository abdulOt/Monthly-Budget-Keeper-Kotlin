package com.talent.monthlybudgetkeeper.utils

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.math.min

object PinSecurity {
    private const val SALT_SIZE_BYTES = 16
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256

    fun normalizePin(pin: String): String = pin.filter(Char::isDigit)

    fun isValidPin(pin: String): Boolean = normalizePin(pin).length in 4..8

    fun hashPin(pin: String): String {
        val normalized = normalizePin(pin)
        require(isValidPin(normalized)) { "PIN must be 4 to 8 digits." }
        val salt = ByteArray(SALT_SIZE_BYTES).also(SecureRandom()::nextBytes)
        val derived = derive(normalized, salt)
        return listOf(
            Base64.encodeToString(salt, Base64.NO_WRAP),
            Base64.encodeToString(derived, Base64.NO_WRAP)
        ).joinToString(":")
    }

    fun verify(pin: String, expectedHash: String?): Boolean {
        if (expectedHash.isNullOrBlank()) return false
        val normalized = normalizePin(pin)
        if (!isValidPin(normalized)) return false

        val parts = expectedHash.split(':')
        if (parts.size != 2) return false

        val salt = runCatching { Base64.decode(parts[0], Base64.NO_WRAP) }.getOrNull() ?: return false
        val expected = runCatching { Base64.decode(parts[1], Base64.NO_WRAP) }.getOrNull() ?: return false
        val actual = derive(normalized, salt)
        return constantTimeEquals(expected, actual)
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val keySpec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(keySpec).encoded
    }

    private fun constantTimeEquals(expected: ByteArray, actual: ByteArray): Boolean {
        if (expected.size != actual.size) return false
        var result = 0
        for (index in 0 until min(expected.size, actual.size)) {
            result = result or (expected[index].toInt() xor actual[index].toInt())
        }
        return result == 0
    }
}
