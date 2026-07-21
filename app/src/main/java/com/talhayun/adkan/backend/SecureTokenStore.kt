package com.talhayun.adkan.backend

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

// [SKILL-DECL] Ported from iOS App/Backend/KeychainHelper.swift (per
// plan/serialized-tinkering-pony-agent-a78532b0c00bea6c9.md), backed by
// EncryptedSharedPreferences — the Android analog of Keychain for small
// token-sized secrets.
//
// NOTE (explicit trade-off, called out in the plan): androidx.security:
// security-crypto was marked deprecated as of 1.1.0-alpha07 (April 2025).
// Google's long-term recommendation is Jetpack DataStore + raw Tink, which is
// materially more code/surface to get right without a compiler available in
// this environment. We're using the last stable-ish alpha (1.1.0-alpha06)
// for this pass — it still ships and functions — and flagging the future
// migration to DataStore+Tink as a follow-up rather than silently picking one.
object SecureTokenStore {
    private const val PREFS_FILE_NAME = "com.talhayun.adkan.secure_prefs"

    private fun prefs(context: Context): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            PREFS_FILE_NAME,
            masterKeyAlias,
            context.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun save(context: Context, key: String, value: String) {
        prefs(context).edit().putString(key, value).apply()
    }

    fun read(context: Context, key: String): String? =
        prefs(context).getString(key, null)

    fun delete(context: Context, key: String) {
        prefs(context).edit().remove(key).apply()
    }
}
