package com.geradorplus.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.geradorplus.data.models.User
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "geradorplus_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val gson = Gson()

    companion object {
        private const val KEY_USER = "logged_user"
        private const val KEY_LOGIN_TIME = "login_time"
        private const val KEY_REMEMBER = "remember_me"
    }

    fun saveSession(user: User, rememberMe: Boolean = false) {
        prefs.edit()
            .putString(KEY_USER, gson.toJson(user))
            .putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
            .putBoolean(KEY_REMEMBER, rememberMe)
            .apply()
    }

    fun getLoggedUser(): User? {
        val json = prefs.getString(KEY_USER, null) ?: return null
        return try {
            gson.fromJson(json, User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun isLoggedIn(): Boolean {
        val user = getLoggedUser() ?: return false
        return !user.isExpired() && user.isActive
    }

    fun isRememberMe(): Boolean = prefs.getBoolean(KEY_REMEMBER, false)

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun updateUser(user: User) {
        prefs.edit().putString(KEY_USER, gson.toJson(user)).apply()
    }
}
