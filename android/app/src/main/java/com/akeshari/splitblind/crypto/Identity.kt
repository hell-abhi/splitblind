package com.akeshari.splitblind.crypto

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

class Identity(context: Context) {
    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            context, "splitblind_identity", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var memberId: String
        get() = prefs.getString("member_id", null) ?: run {
            val id = UUID.randomUUID().toString().take(16)
            prefs.edit().putString("member_id", id).apply()
            id
        }
        set(value) = prefs.edit().putString("member_id", value).apply()

    var displayName: String
        get() = prefs.getString("display_name", "") ?: ""
        set(value) = prefs.edit().putString("display_name", value).apply()

    var recoveryPassphrase: String?
        get() = prefs.getString("recovery_passphrase", null)
        set(value) = prefs.edit().putString("recovery_passphrase", value).apply()

    var personalGroupId: String?
        get() = prefs.getString("personal_group_id", null)
        set(value) = prefs.edit().putString("personal_group_id", value).apply()

    var defaultCurrency: String
        get() = prefs.getString("default_currency", "INR") ?: "INR"
        set(value) = prefs.edit().putString("default_currency", value).apply()

    val isOnboarded: Boolean get() = displayName.isNotBlank()
}
