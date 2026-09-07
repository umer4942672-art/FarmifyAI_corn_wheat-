package com.example.data.local

import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Password hashing for the offline Room fallback.
 * PBKDF2 is used instead of storing a plaintext password.
 *
 * Stored format: pbkdf2-sha256$iterations$saltBase64$hashBase64
 */
object PasswordHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16
    private const val PREFIX = "pbkdf2-sha256"

    fun hash(password: String): String {
        require(password.isNotEmpty()) { "Password cannot be empty" }
        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val derived = derive(password, salt, ITERATIONS)
        return "${PREFIX}${'$'}${ITERATIONS}${'$'}${b64(salt)}${'$'}${b64(derived)}"
    }

    fun verify(password: String, stored: String): Boolean {
        if (password.isEmpty() || stored.isBlank()) return false
        val parts = stored.split('$')
        if (parts.size != 4 || parts[0] != PREFIX) return false
        return try {
            val iterations = parts[1].toInt()
            val salt = Base64.decode(parts[2], Base64.NO_WRAP)
            val expected = Base64.decode(parts[3], Base64.NO_WRAP)
            val actual = derive(password, salt, iterations)
            MessageDigest.isEqual(actual, expected)
        } catch (_: Exception) {
            false
        }
    }

    /** One-time compatibility path for older builds that stored plaintext locally. */
    fun isLegacyPlaintextMatch(password: String, stored: String): Boolean =
        stored.isNotBlank() && !stored.startsWith(PREFIX + "$") && stored == password

    private fun derive(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun b64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
}
