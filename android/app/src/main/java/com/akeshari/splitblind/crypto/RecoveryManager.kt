package com.akeshari.splitblind.crypto

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import com.akeshari.splitblind.data.database.dao.GroupDao
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.coroutines.resume

@Serializable
data class RecoveryBackupData(
    val memberId: String,
    val displayName: String,
    val groups: List<RecoveryGroupData>
)

@Serializable
data class RecoveryGroupData(
    val groupId: String,
    val name: String,
    val groupKeyBase64: String,
    val createdBy: String,
    val createdAt: Long
)

object RecoveryManager {
    private const val TAG = "RecoveryManager"
    private const val SALT_SUFFIX = "splitblind-recovery"
    private const val GCM_TAG_LENGTH = 128
    private const val NONCE_LENGTH = 12

    private val json = Json { ignoreUnknownKeys = true }

    val wordList = listOf(
        "apple", "mango", "banana", "cherry", "grape", "melon", "peach", "plum",
        "lemon", "olive", "berry", "kiwi", "fig", "pear", "lime", "coconut",
        "ocean", "river", "cloud", "storm", "breeze", "flame", "frost", "thunder",
        "shadow", "crystal", "silver", "golden", "violet", "coral", "amber", "jade",
        "tiger", "eagle", "dolphin", "falcon", "phoenix", "dragon", "wolf", "panther",
        "sunset", "horizon", "meadow", "garden", "forest", "valley", "summit", "island",
        "castle", "bridge", "tower", "anchor", "compass", "lantern", "beacon", "mirror",
        "rhythm", "melody", "harmony", "tempo", "echo", "whisper", "thunder", "chorus",
        "brave", "swift", "gentle", "vivid", "calm", "bright", "noble", "wild",
        "spark", "bloom", "drift", "quest", "pulse", "gleam", "haven", "crest"
    )

    fun generatePassphrase(): String {
        val random = SecureRandom()
        val words = mutableListOf<String>()
        val used = mutableSetOf<Int>()
        while (words.size < 3) {
            val idx = random.nextInt(wordList.size)
            if (idx !in used) {
                used.add(idx)
                words.add(wordList[idx])
            }
        }
        return words.joinToString(" ")
    }

    fun validatePassphrase(passphrase: String): Boolean {
        val words = passphrase.trim().split("\\s+".toRegex())
        return words.size >= 3 && passphrase.trim().length >= 8
    }

    fun ppHash(passphrase: String): String {
        val input = passphrase.lowercase().trim() + SALT_SUFFIX
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        val b64 = Base64.encodeToString(hash, Base64.NO_WRAP)
        return b64.filter { it.isLetterOrDigit() }.take(24)
    }

    fun derivePpKey(passphrase: String, salt: String): SecretKey {
        val input = passphrase.lowercase().trim() + salt
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun encryptWithPassphrase(passphrase: String, data: String): Triple<String, String, String> {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)

        val key = derivePpKey(passphrase, saltBase64)
        val nonce = ByteArray(NONCE_LENGTH)
        SecureRandom().nextBytes(nonce)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, nonce))
        val ct = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

        return Triple(
            saltBase64,
            Base64.encodeToString(ct, Base64.NO_WRAP),
            Base64.encodeToString(nonce, Base64.NO_WRAP)
        )
    }

    private fun decryptWithPassphrase(passphrase: String, saltBase64: String, ciphertextBase64: String, nonceBase64: String): String? {
        return try {
            val key = derivePpKey(passphrase, saltBase64)
            val nonce = Base64.decode(nonceBase64, Base64.NO_WRAP)
            val ct = Base64.decode(ciphertextBase64, Base64.NO_WRAP)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, nonce))
            String(cipher.doFinal(ct), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "Decryption failed (expected for wrong passphrase)", e)
            null
        }
    }

    suspend fun backupToRecovery(
        passphrase: String,
        identity: Identity,
        groupDao: GroupDao,
        firebase: FirebaseDatabase
    ) {
        val groups = groupDao.getAllGroupsList()
        val backupData = RecoveryBackupData(
            memberId = identity.memberId,
            displayName = identity.displayName,
            groups = groups.map { g ->
                RecoveryGroupData(
                    groupId = g.groupId,
                    name = g.name,
                    groupKeyBase64 = g.groupKeyBase64,
                    createdBy = g.createdBy,
                    createdAt = g.createdAt
                )
            }
        )

        val plaintext = json.encodeToString(backupData)
        val (salt, ciphertext, nonce) = encryptWithPassphrase(passphrase, plaintext)
        val hash = ppHash(passphrase)

        val entry = mapOf(
            "s" to salt,
            "d" to ciphertext,
            "n" to nonce,
            "ts" to System.currentTimeMillis()
        )

        firebase.getReference("recovery/$hash").push().setValue(entry)
            .addOnFailureListener { e -> Log.e(TAG, "Failed to push recovery backup", e) }
    }

    suspend fun recoverFromPassphrase(
        passphrase: String,
        firebase: FirebaseDatabase
    ): RecoveryBackupData? = suspendCancellableCoroutine { cont ->
        val hash = ppHash(passphrase)
        firebase.getReference("recovery/$hash").get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    cont.resume(null)
                    return@addOnSuccessListener
                }

                for (child in snapshot.children) {
                    val salt = child.child("s").getValue(String::class.java) ?: continue
                    val ciphertext = child.child("d").getValue(String::class.java) ?: continue
                    val nonce = child.child("n").getValue(String::class.java) ?: continue

                    val plaintext = decryptWithPassphrase(passphrase, salt, ciphertext, nonce)
                    if (plaintext != null) {
                        try {
                            val data = json.decodeFromString<RecoveryBackupData>(plaintext)
                            cont.resume(data)
                            return@addOnSuccessListener
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to parse recovery data", e)
                        }
                    }
                }
                cont.resume(null)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to fetch recovery data", e)
                cont.resume(null)
            }
    }

    suspend fun autoBackup(
        identity: Identity,
        groupDao: GroupDao,
        firebase: FirebaseDatabase
    ) {
        val passphrase = identity.recoveryPassphrase ?: return
        val hash = ppHash(passphrase)

        // Remove old entries then push fresh
        suspendCancellableCoroutine<Unit> { cont ->
            firebase.getReference("recovery/$hash").removeValue()
                .addOnCompleteListener { cont.resume(Unit) }
        }
        backupToRecovery(passphrase, identity, groupDao, firebase)
    }

    fun downloadPassphrase(context: Context, passphrase: String) {
        val content = """SplitBlind Recovery Passphrase
================================
$passphrase
================================
Keep this safe! You need these words to recover your SplitBlind data on a new device.
"""
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "splitblind-recovery.txt")
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { os ->
                os.write(content.toByteArray())
            }
        }
    }

    fun sharePassphrase(context: Context, passphrase: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "SplitBlind Recovery Passphrase")
            putExtra(Intent.EXTRA_TEXT, "My SplitBlind recovery passphrase: $passphrase")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share Passphrase").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
