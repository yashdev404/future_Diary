package com.example.futurediary.ui.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import java.util.*

object SecurityUtils {

    private const val PREFS_NAME = "secure_prefs"
    private const val DB_PASSPHRASE_KEY = "db_passphrase"

    /**
     * Gets or creates a high-entropy passphrase for the database.
     * The passphrase is stored in EncryptedSharedPreferences, which is
     * backed by the Android Keystore (hardware-backed).
     */
    fun getDatabasePassphrase(context: Context): ByteArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val storedPassphrase = sharedPreferences.getString(DB_PASSPHRASE_KEY, null)
        return if (storedPassphrase != null) {
            Base64.getDecoder().decode(storedPassphrase)
        } else {
            val newPassphrase = ByteArray(32)
            SecureRandom().nextBytes(newPassphrase)
            val encoded = Base64.getEncoder().encodeToString(newPassphrase)
            sharedPreferences.edit().putString(DB_PASSPHRASE_KEY, encoded).apply()
            newPassphrase
        }
    }
}
