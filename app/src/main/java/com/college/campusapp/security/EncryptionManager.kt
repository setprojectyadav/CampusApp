package com.college.campusapp.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object EncryptionManager {
    private const val PREFS_FILE = "secure_campus_prefs"

    private fun getEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return try {
            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            try {
                context.deleteSharedPreferences(PREFS_FILE)
            } catch (ignored: Exception) {}
            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    fun saveString(context: Context, key: String, value: String) {
        getEncryptedPrefs(context).edit().putString(key, value).apply()
    }

    fun getString(context: Context, key: String, defaultValue: String? = null): String? {
        return getEncryptedPrefs(context).getString(key, defaultValue)
    }

    fun saveBoolean(context: Context, key: String, value: Boolean) {
        getEncryptedPrefs(context).edit().putBoolean(key, value).apply()
    }

    fun getBoolean(context: Context, key: String, defaultValue: Boolean = false): Boolean {
        return getEncryptedPrefs(context).getBoolean(key, defaultValue)
    }

    fun clear(context: Context) {
        getEncryptedPrefs(context).edit().clear().apply()
    }
}
