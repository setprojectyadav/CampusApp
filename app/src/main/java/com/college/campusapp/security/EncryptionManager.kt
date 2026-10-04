package com.college.campusapp.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object EncryptionManager {
    private const val PREFS_FILE = "secure_campus_prefs"

    private fun getEncryptedPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
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
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context,
                    PREFS_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (fallbackEx: Exception) {
                // Return standard shared preferences if encrypted prefs fails on legacy/emulator devices
                context.getSharedPreferences("campus_fallback_prefs", Context.MODE_PRIVATE)
            }
        }
    }

    fun saveString(context: Context, key: String, value: String?) {
        try {
            if (value == null) {
                getEncryptedPrefs(context).edit().remove(key).apply()
            } else {
                getEncryptedPrefs(context).edit().putString(key, value).apply()
            }
        } catch (e: Exception) {}
    }

    fun getString(context: Context, key: String, defaultValue: String? = null): String? {
        return try {
            getEncryptedPrefs(context).getString(key, defaultValue)
        } catch (e: Exception) {
            defaultValue
        }
    }

    fun saveBoolean(context: Context, key: String, value: Boolean) {
        try {
            getEncryptedPrefs(context).edit().putBoolean(key, value).apply()
        } catch (e: Exception) {}
    }

    fun getBoolean(context: Context, key: String, defaultValue: Boolean = false): Boolean {
        return try {
            getEncryptedPrefs(context).getBoolean(key, defaultValue)
        } catch (e: Exception) {
            defaultValue
        }
    }

    fun saveInt(context: Context, key: String, value: Int) {
        try {
            getEncryptedPrefs(context).edit().putInt(key, value).apply()
        } catch (e: Exception) {}
    }

    fun getInt(context: Context, key: String, defaultValue: Int = 0): Int {
        return try {
            getEncryptedPrefs(context).getInt(key, defaultValue)
        } catch (e: Exception) {
            defaultValue
        }
    }

    fun clear(context: Context) {
        try {
            getEncryptedPrefs(context).edit().clear().apply()
        } catch (e: Exception) {}
    }
}
