package com.ayamq.kiosk.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

/**
 * Manager for SharedPreferences operations
 * Handles persistent storage of app settings
 */
class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "ayamq_preferences"
        private const val KEY_ADMIN_PIN = "admin_pin"

        private const val KEY_LAST_BACKUP = "last_backup"

        const val DEFAULT_ADMIN_PIN = "1234"
    }


    /**
     * Get admin PIN
     * @return The stored PIN or default PIN
     */
    fun getAdminPin(): String {
        return prefs.getString(KEY_ADMIN_PIN, DEFAULT_ADMIN_PIN) ?: DEFAULT_ADMIN_PIN
    }

    /**
     * Verify if entered PIN is correct
     * @param enteredPin The PIN entered by user
     * @return true if PIN matches
     */
    fun verifyAdminPin(enteredPin: String): Boolean {
        return enteredPin == getAdminPin()
    }

    /**
     * Save last backup timestamp
     * @param timestamp Milliseconds since epoch
     */
    @SuppressLint("UseKtx")
    fun saveLastBackupTime(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_BACKUP, timestamp).apply()
    }
}