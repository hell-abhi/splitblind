package com.akeshari.splitblind.crypto

import android.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Serializable
data class EncryptedEnvelope(val d: String, val n: String)

object CryptoEngine {
    private const val GCM_TAG_LENGTH = 128
    private const val NONCE_LENGTH = 12

    fun generateGroupKey(): String {
        val keyBytes = ByteArray(32)
        SecureRandom().nextBytes(keyBytes)
        return Base64.encodeToString(keyBytes, Base64.NO_WRAP)
    }

    fun encrypt(keyBase64: String, data: String): EncryptedEnvelope {
        val keyBytes = Base64.decode(keyBase64, Base64.NO_WRAP)
        val nonce = ByteArray(NONCE_LENGTH)
        SecureRandom().nextBytes(nonce)
        val key = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, nonce))
        val ct = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        return EncryptedEnvelope(
            d = Base64.encodeToString(ct, Base64.NO_WRAP),
            n = Base64.encodeToString(nonce, Base64.NO_WRAP)
        )
    }

    fun decrypt(keyBase64: String, envelope: EncryptedEnvelope): String {
        val keyBytes = Base64.decode(keyBase64, Base64.NO_WRAP)
        val nonce = Base64.decode(envelope.n, Base64.NO_WRAP)
        val ct = Base64.decode(envelope.d, Base64.NO_WRAP)
        val key = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, nonce))
        return String(cipher.doFinal(ct), Charsets.UTF_8)
    }
}
