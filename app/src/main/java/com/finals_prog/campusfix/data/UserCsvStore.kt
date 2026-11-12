package com.finals_prog.campusfix.data

import android.content.Context
import android.util.Base64
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

enum class SignUpResult { SUCCESS, EMAIL_TAKEN }

/**
 * Stores users in <app internal storage>/users.csv
 * Row format: email,salt,passwordHash
 * Passwords are never stored in plain text.
 */
class UserCsvStore(context: Context) {

    private val file = File(context.filesDir, "users.csv")

    private data class UserRow(val email: String, val salt: String, val hash: String,val role: String)

    @Synchronized
    fun register(email: String, password: String, role: String): SignUpResult {
        val normalized = normalize(email)
        if (readUsers().any { it.email == normalized }) return SignUpResult.EMAIL_TAKEN

        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashB64 = hash(password, salt)

        file.appendText("$normalized,$saltB64,$hashB64,$role\n")
        return SignUpResult.SUCCESS
    }

    @Synchronized
    fun authenticate(email: String, password: String): Boolean {
        val user = readUsers().firstOrNull { it.email == normalize(email) } ?: return false
        val salt = Base64.decode(user.salt, Base64.NO_WRAP)
        val attempt = hash(password, salt)
        // Constant-time comparison
        return MessageDigest.isEqual(attempt.toByteArray(), user.hash.toByteArray())
    }

    private fun readUsers(): List<UserRow> {
        if (!file.exists()) return emptyList()
        return file.readLines().mapNotNull { line ->
            val parts = line.split(",")
            if (parts.size == 3) UserRow(parts[0], parts[1], parts[2],parts[3]) else null
        }
    }

    private fun normalize(email: String) = email.trim().lowercase()

    private fun hash(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, 10_000, 256)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            .generateSecret(spec).encoded
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}