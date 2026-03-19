package com.akeshari.splitblind.sync

import android.util.Base64
import com.akeshari.splitblind.crypto.Identity
import com.akeshari.splitblind.data.database.dao.GroupDao
import com.akeshari.splitblind.data.database.entity.GroupEntity
import com.akeshari.splitblind.data.database.entity.MemberEntity
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class SyncGroup(
    val i: String,
    val k: String,
    val n: String
)

@Serializable
data class SyncBundle(
    val id: String,
    val name: String,
    val groups: List<SyncGroup>
)

@Singleton
class SyncDeviceManager @Inject constructor(
    private val identity: Identity,
    private val groupDao: GroupDao,
    private val firebaseDatabase: FirebaseDatabase
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val syncChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun generateSyncCode(): String {
        val random = SecureRandom()
        val part1 = (1..4).map { syncChars[random.nextInt(syncChars.length)] }.joinToString("")
        val part2 = (1..4).map { syncChars[random.nextInt(syncChars.length)] }.joinToString("")
        return "$part1-$part2"
    }

    private fun deriveKey(pin: String): SecretKeySpec {
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest((pin + "splitblind-salt-2026").toByteArray(Charsets.UTF_8))
        return SecretKeySpec(hash, "AES")
    }

    suspend fun generateSyncBundle(pin: String): ByteArray {
        val groups = groupDao.getAllGroupsList()
        val syncGroups = groups.map { g ->
            SyncGroup(i = g.groupId, k = g.groupKeyBase64, n = g.name)
        }
        val bundle = SyncBundle(
            id = identity.memberId,
            name = identity.displayName,
            groups = syncGroups
        )
        val plaintext = json.encodeToString(bundle).toByteArray(Charsets.UTF_8)
        return encrypt(plaintext, pin)
    }

    private fun encrypt(plaintext: ByteArray, pin: String): ByteArray {
        val key = deriveKey(pin)
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(plaintext)
        // Return iv + ciphertext
        return iv + ciphertext
    }

    fun restoreFromBundle(encBundle: ByteArray, pin: String): SyncBundle {
        val key = deriveKey(pin)
        val iv = encBundle.copyOfRange(0, 12)
        val ciphertext = encBundle.copyOfRange(12, encBundle.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        val plaintext = cipher.doFinal(ciphertext)
        return json.decodeFromString(String(plaintext, Charsets.UTF_8))
    }

    suspend fun uploadSyncBundle(code: String, encBundle: ByteArray) {
        val key = code.replace("-", "")
        val iv = encBundle.copyOfRange(0, 12)
        val ciphertext = encBundle.copyOfRange(12, encBundle.size)
        val data = mapOf(
            "d" to Base64.encodeToString(ciphertext, Base64.NO_WRAP),
            "n" to Base64.encodeToString(iv, Base64.NO_WRAP),
            "ts" to ServerValue.TIMESTAMP
        )
        firebaseDatabase.reference.child("sync").child(key).setValue(data).await()
    }

    suspend fun downloadSyncBundle(code: String): ByteArray? {
        val key = code.replace("-", "")
        val snapshot = firebaseDatabase.reference.child("sync").child(key).get().await()
        if (!snapshot.exists()) return null

        val d = snapshot.child("d").getValue(String::class.java) ?: return null
        val n = snapshot.child("n").getValue(String::class.java) ?: return null
        val ts = snapshot.child("ts").getValue(Long::class.java) ?: return null

        // Check if expired (5 minutes)
        val now = System.currentTimeMillis()
        if (now - ts > 5 * 60 * 1000) {
            // Clean up expired entry
            firebaseDatabase.reference.child("sync").child(key).removeValue()
            return null
        }

        val iv = Base64.decode(n, Base64.NO_WRAP)
        val ciphertext = Base64.decode(d, Base64.NO_WRAP)
        return iv + ciphertext
    }

    suspend fun applyRestore(bundle: SyncBundle) {
        identity.memberId = bundle.id
        identity.displayName = bundle.name

        val now = System.currentTimeMillis()
        for (group in bundle.groups) {
            val existing = groupDao.getGroup(group.i)
            if (existing == null) {
                groupDao.insertGroup(
                    GroupEntity(
                        groupId = group.i,
                        name = group.n,
                        createdBy = bundle.id,
                        createdAt = now,
                        groupKeyBase64 = group.k,
                        hlcTimestamp = now
                    )
                )
            }
            groupDao.insertMember(
                MemberEntity(
                    groupId = group.i,
                    memberId = bundle.id,
                    displayName = bundle.name,
                    joinedAt = now,
                    hlcTimestamp = now
                )
            )
        }
    }

    suspend fun deleteSyncEntry(code: String) {
        val key = code.replace("-", "")
        firebaseDatabase.reference.child("sync").child(key).removeValue().await()
    }
}
