package com.infiniteloop.cyclefollower.backup

import com.infiniteloop.cyclefollower.data.UserProfile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Reads and writes the whole profile as one file.
 *
 * Optionally password-protected, and that is not decoration: an unencrypted export sitting in the
 * Downloads folder is exactly the leak the rest of the app's discretion is trying to avoid.
 */
object BackupCodec {

    private const val MAGIC = "cyclefollower.backup"
    const val FORMAT_VERSION = 1

    private const val PBKDF2_ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val GCM_TAG_BITS = 128
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }
    private val compact = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    data class Envelope(
        val magic: String = MAGIC,
        val version: Int = FORMAT_VERSION,
        val encrypted: Boolean = false,
        /** Profile JSON when plain; base64 ciphertext when encrypted. */
        val payload: String,
        val salt: String? = null,
        val iv: String? = null,
    )

    sealed interface Result {
        data class Ok(val profile: UserProfile) : Result
        data object NotABackup : Result
        data object NeedsPassword : Result
        data object WrongPassword : Result
        data class Unreadable(val reason: String) : Result
    }

    fun encode(profile: UserProfile, password: String? = null): String {
        val body = compact.encodeToString(UserProfile.serializer(), profile)
        if (password.isNullOrEmpty()) {
            return json.encodeToString(Envelope.serializer(), Envelope(payload = body))
        }
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, keyFrom(password, salt), GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        val sealed = cipher.doFinal(body.toByteArray(Charsets.UTF_8))
        return json.encodeToString(
            Envelope.serializer(),
            Envelope(
                encrypted = true,
                payload = b64(sealed),
                salt = b64(salt),
                iv = b64(iv),
            ),
        )
    }

    fun decode(text: String, password: String? = null): Result {
        val envelope = runCatching { json.decodeFromString(Envelope.serializer(), text) }.getOrNull()
            ?: return Result.NotABackup
        if (envelope.magic != MAGIC) return Result.NotABackup
        if (envelope.version > FORMAT_VERSION) {
            return Result.Unreadable("This backup was written by a newer version of the app.")
        }

        val body = if (!envelope.encrypted) {
            envelope.payload
        } else {
            if (password.isNullOrEmpty()) return Result.NeedsPassword
            val salt = envelope.salt?.let(::unb64) ?: return Result.Unreadable("The backup is missing its salt.")
            val iv = envelope.iv?.let(::unb64) ?: return Result.Unreadable("The backup is missing its iv.")
            val opened = runCatching {
                Cipher.getInstance("AES/GCM/NoPadding").apply {
                    init(Cipher.DECRYPT_MODE, keyFrom(password, salt), GCMParameterSpec(GCM_TAG_BITS, iv))
                }.doFinal(unb64(envelope.payload))
            }.getOrNull() ?: return Result.WrongPassword
            String(opened, Charsets.UTF_8)
        }

        val profile = runCatching { compact.decodeFromString(UserProfile.serializer(), body) }.getOrNull()
            ?: return Result.Unreadable("The contents did not read as a profile.")
        return Result.Ok(profile.normalised())
    }

    /** True if the file needs a password, without attempting to open it. */
    fun isEncrypted(text: String): Boolean =
        runCatching { json.decodeFromString(Envelope.serializer(), text).encrypted }.getOrDefault(false)

    private fun keyFrom(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_BITS)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return SecretKeySpec(bytes, "AES")
    }

    // java.util.Base64 rather than android.util.Base64: available from API 26 (our floor) and,
    // unlike the Android one, real on the JVM so the round trip can be unit tested.
    private fun b64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    private fun unb64(text: String): ByteArray = Base64.getDecoder().decode(text)

    fun suggestedFileName(): String = "cycle-follower-backup.json"
}
