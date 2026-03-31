package com.smartamenities.data.local

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.smartamenities.data.model.User
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local user store backed by SharedPreferences + Gson.
 *
 * Two separate keys are used:
 *  - KEY_USERS       : JSON array of all registered (non-guest) accounts
 *  - KEY_SESSION_JSON: full JSON of the currently active user
 *                      (could be a registered user or a guest)
 *
 * This class is the single source of truth for auth in Iteration 1.
 * Replace with a remote auth service by swapping the Hilt binding in AppModule.
 */
@Singleton
class UserDataStore @Inject constructor(
    private val prefs: SharedPreferences
) {
    private val gson = Gson()

    private companion object {
        const val KEY_USERS        = "sa_users_list"
        const val KEY_SESSION_JSON = "sa_current_user_json"
    }

    // ── Password hashing ──────────────────────────────────────────────────────

    fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    // ── User registry (registered accounts only, no guests) ───────────────────

    private fun loadUsers(): MutableList<User> {
        val json = prefs.getString(KEY_USERS, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<User>>() {}.type
        return try { gson.fromJson(json, type) ?: mutableListOf() }
               catch (_: Exception) { mutableListOf() }
    }

    private fun saveUsers(users: List<User>) {
        prefs.edit().putString(KEY_USERS, gson.toJson(users)).apply()
    }

    /**
     * Registers a new account.
     * Returns [Result.failure] if the email is already taken.
     */
    fun createUser(user: User): Result<User> {
        val users = loadUsers()
        if (users.any { it.email.equals(user.email, ignoreCase = true) }) {
            return Result.failure(Exception("An account with this email already exists."))
        }
        users.add(user)
        saveUsers(users)
        return Result.success(user)
    }

    /** Looks up a registered account by email + password hash. */
    fun authenticate(email: String, passwordHash: String): User? =
        loadUsers().find {
            it.email.equals(email, ignoreCase = true) && it.passwordHash == passwordHash
        }

    // ── Session management ────────────────────────────────────────────────────

    /** Persist the active session (works for both registered users and guests). */
    fun saveSession(user: User) {
        prefs.edit().putString(KEY_SESSION_JSON, gson.toJson(user)).apply()
    }

    /** Restore the session from disk, or null if no session exists. */
    fun restoreSession(): User? {
        val json = prefs.getString(KEY_SESSION_JSON, null) ?: return null
        return try { gson.fromJson(json, User::class.java) } catch (_: Exception) { null }
    }

    fun clearSession() {
        prefs.edit().remove(KEY_SESSION_JSON).apply()
    }

    fun hasActiveSession(): Boolean = restoreSession() != null
}
