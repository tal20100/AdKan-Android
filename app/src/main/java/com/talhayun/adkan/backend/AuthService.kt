package com.talhayun.adkan.backend

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// [SKILL-DECL] Ported 1:1 from iOS App/Backend/AuthService.swift (per
// plan/serialized-tinkering-pony-agent-a78532b0c00bea6c9.md), swapping Apple
// Sign-In's identityToken for the Google ID token obtained via
// GoogleIdTokenProvider, and using the same
// `POST /auth/v1/token?grant_type=id_token` Supabase endpoint with
// `{"provider": "google", "id_token": ...}` (iOS uses `"provider": "apple"`).
// Networking uses HttpURLConnection + org.json — both built into Android, no
// new HTTP client dependency, matching this repo's minimalist-dependency style.

interface AuthService {
    val currentUserId: String?
    val isAuthenticated: Boolean
    val authStateChanges: SharedFlow<Unit>

    suspend fun signInWithGoogle(idToken: String)
    fun signOut()
    suspend fun accessToken(): String?
    suspend fun refreshSession()
    suspend fun deleteAccount()
    suspend fun updateProfile(displayName: String, avatarEmoji: String, goalMinutes: Int = 0)
    suspend fun ensureUserRow()
    suspend fun syncPremiumStatus(isPremium: Boolean, isTrial: Boolean)
}

class AuthError(message: String) : Exception(message) {
    companion object {
        fun missingToken() = AuthError("Google Sign-In did not provide a token.")
        fun serverError() = AuthError("Authentication server error.")
        fun invalidResponse() = AuthError("Unexpected server response.")
    }
}

class SupabaseAuthService(
    private val appContext: Context,
    private val baseURL: String,
    private val apiKey: String,
) : AuthService {

    private val tokenKey = "com.talhayun.adkan.accessToken"
    private val refreshTokenKey = "com.talhayun.adkan.refreshToken"
    private val userIdKey = "com.talhayun.adkan.userId"
    private val hasEverAuthenticatedKey = "com.talhayun.adkan.hasEverAuthenticated"

    // Non-secret app state (user id, small flags) lives in plain
    // SharedPreferences — same split as iOS, which keeps userId in
    // UserDefaults and only the tokens in Keychain.
    private val plainPrefs = appContext.applicationContext
        .getSharedPreferences("com.talhayun.adkan.auth_state", Context.MODE_PRIVATE)

    private val _authStateChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val authStateChanges: SharedFlow<Unit> = _authStateChanges.asSharedFlow()

    init {
        clearOrphanedTokens()
    }

    private fun clearOrphanedTokens() {
        if (plainPrefs.getString(userIdKey, null) == null) {
            SecureTokenStore.delete(appContext, tokenKey)
            SecureTokenStore.delete(appContext, refreshTokenKey)
        }
    }

    override val currentUserId: String?
        get() = plainPrefs.getString(userIdKey, null)

    override val isAuthenticated: Boolean
        get() = currentUserId != null && SecureTokenStore.read(appContext, tokenKey) != null

    // MARK: - SIGN IN

    override suspend fun signInWithGoogle(idToken: String) = withContext(Dispatchers.IO) {
        if (idToken.isBlank()) throw AuthError.missingToken()

        val url = URL("$baseURL/auth/v1/token?grant_type=id_token")
        val body = JSONObject().apply {
            put("provider", "google")
            put("id_token", idToken)
        }

        val json = postJson(url, body)
            ?: throw AuthError.serverError()

        val accessToken = json.optString("access_token", "")
        val user = json.optJSONObject("user")
        val userId = user?.optString("id", "") ?: ""

        if (accessToken.isBlank() || userId.isBlank()) {
            throw AuthError.invalidResponse()
        }

        SecureTokenStore.save(appContext, tokenKey, accessToken)

        val refreshToken = json.optString("refresh_token", "")
        if (refreshToken.isNotBlank()) {
            SecureTokenStore.save(appContext, refreshTokenKey, refreshToken)
        }

        plainPrefs.edit().putString(userIdKey, userId).apply()

        ensureUserRow()

        // IMPORTANT: DB becomes source of truth immediately (mirrors iOS).
        fetchAndRestoreProfile()

        plainPrefs.edit().putBoolean(hasEverAuthenticatedKey, true).apply()

        _authStateChanges.tryEmit(Unit)
        Unit
    }

    // MARK: - PROFILE FETCH (DB wins)

    private suspend fun fetchAndRestoreProfile() = withContext(Dispatchers.IO) {
        val token = SecureTokenStore.read(appContext, tokenKey) ?: return@withContext
        val uid = currentUserId ?: return@withContext

        val url = URL("$baseURL/rest/v1/users?select=*&id=eq.$uid")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("apikey", apiKey)
            setRequestProperty("Authorization", "Bearer $token")
        }

        try {
            val code = connection.responseCode
            if (code !in 200..299) return@withContext

            val rows = org.json.JSONArray(connection.inputStream.bufferedReader().readText())
            if (rows.length() == 0) return@withContext
            val row = rows.getJSONObject(0)

            val serverName = row.optString("display_name", "")
            val serverEmoji = row.optString("avatar_emoji", "")
            val serverProfileCompleted = row.optBoolean("profile_completed", false)

            val editor = plainPrefs.edit()
            // Server wins for real display name data. Never overwrite local
            // value with empty — a brand-new account has NULL on server.
            if (serverName.isNotEmpty()) editor.putString("profileDisplayName", serverName)
            if (serverEmoji.isNotEmpty()) editor.putString("profileAvatarEmoji", serverEmoji)
            // Always mirror server truth — covers shared-device re-login.
            editor.putBoolean("profileCompleted", serverProfileCompleted)
            editor.apply()
        } catch (_: Exception) {
            return@withContext
        } finally {
            connection.disconnect()
        }
    }

    // MARK: - UPDATE PROFILE (only explicit user action)

    override suspend fun updateProfile(displayName: String, avatarEmoji: String, goalMinutes: Int) =
        withContext(Dispatchers.IO) {
            if (!isAuthenticated) return@withContext
            val token = SecureTokenStore.read(appContext, tokenKey) ?: return@withContext

            val url = URL("$baseURL/rest/v1/rpc/update_profile")
            val body = JSONObject().apply {
                put("new_display_name", displayName)
                put("new_avatar_emoji", avatarEmoji)
                if (goalMinutes > 0) put("new_daily_goal_minutes", goalMinutes)
            }

            postJsonWithToken(url, body, token) ?: throw AuthError.serverError()
            Unit
        }

    // MARK: - OTHER METHODS (unchanged core logic)

    override fun signOut() {
        SecureTokenStore.delete(appContext, tokenKey)
        SecureTokenStore.delete(appContext, refreshTokenKey)
        plainPrefs.edit().remove(userIdKey).apply()
        _authStateChanges.tryEmit(Unit)
    }

    override suspend fun accessToken(): String? = withContext(Dispatchers.IO) {
        if (currentUserId == null) return@withContext null
        val token = SecureTokenStore.read(appContext, tokenKey) ?: return@withContext null
        if (isTokenExpiringSoon(token)) {
            refreshSession()
            return@withContext SecureTokenStore.read(appContext, tokenKey)
        }
        token
    }

    private fun isTokenExpiringSoon(token: String): Boolean {
        val parts = token.split(".")
        if (parts.size != 3) return false
        return try {
            val payload = Base64.decode(
                parts[1],
                Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
            )
            val json = JSONObject(String(payload, Charsets.UTF_8))
            val exp = json.optLong("exp", -1L)
            if (exp < 0) return false
            val nowSeconds = System.currentTimeMillis() / 1000
            nowSeconds > exp - 300
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun refreshSession() = withContext(Dispatchers.IO) {
        if (currentUserId == null) return@withContext
        val refreshToken = SecureTokenStore.read(appContext, refreshTokenKey) ?: return@withContext

        val url = URL("$baseURL/auth/v1/token?grant_type=refresh_token")
        val body = JSONObject().apply { put("refresh_token", refreshToken) }

        val connection = openPostConnection(url, authorized = false)
        try {
            writeBody(connection, body)
            val code = connection.responseCode

            if (code == 400 || code == 401) {
                signOut()
                return@withContext
            }

            if (code !in 200..299) return@withContext

            val json = JSONObject(connection.inputStream.bufferedReader().readText())
            val newToken = json.optString("access_token", "")
            if (newToken.isBlank()) return@withContext

            SecureTokenStore.save(appContext, tokenKey, newToken)
            val newRefreshToken = json.optString("refresh_token", "")
            if (newRefreshToken.isNotBlank()) {
                SecureTokenStore.save(appContext, refreshTokenKey, newRefreshToken)
            }
        } catch (_: Exception) {
            return@withContext
        } finally {
            connection.disconnect()
        }
    }

    override suspend fun deleteAccount() = withContext(Dispatchers.IO) {
        if (!isAuthenticated) throw AuthError.missingToken()
        val token = SecureTokenStore.read(appContext, tokenKey) ?: throw AuthError.missingToken()

        val url = URL("$baseURL/rest/v1/rpc/delete_account")
        postJsonWithToken(url, JSONObject(), token) ?: throw AuthError.serverError()

        signOut()
        plainPrefs.edit().remove(hasEverAuthenticatedKey).apply()
    }

    override suspend fun ensureUserRow() = withContext(Dispatchers.IO) {
        val token = SecureTokenStore.read(appContext, tokenKey) ?: return@withContext
        val url = URL("$baseURL/rest/v1/rpc/ensure_user")
        postJsonWithToken(url, JSONObject(), token) ?: throw AuthError.serverError()
        Unit
    }

    override suspend fun syncPremiumStatus(isPremium: Boolean, isTrial: Boolean) = withContext(Dispatchers.IO) {
        val token = SecureTokenStore.read(appContext, tokenKey) ?: return@withContext
        val url = URL("$baseURL/rest/v1/rpc/sync_premium_status")
        val body = JSONObject().apply {
            put("p_is_premium", isPremium)
            put("p_is_trial", isTrial)
        }
        try {
            postJsonWithToken(url, body, token)
        } catch (_: Exception) {
            // best-effort, mirrors iOS's fire-and-forget `try?` usage
        }
        Unit
    }

    // MARK: - HTTP helpers

    private fun openPostConnection(url: URL, authorized: Boolean, token: String? = null): HttpURLConnection {
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("apikey", apiKey)
            if (authorized && token != null) {
                setRequestProperty("Authorization", "Bearer $token")
            }
        }
    }

    private fun writeBody(connection: HttpURLConnection, body: JSONObject) {
        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }
    }

    /** POST with just the anon apikey header (no bearer token) — used for the token endpoint. */
    private fun postJson(url: URL, body: JSONObject): JSONObject? {
        val connection = openPostConnection(url, authorized = false)
        return try {
            writeBody(connection, body)
            val code = connection.responseCode
            if (code !in 200..299) return null
            JSONObject(connection.inputStream.bufferedReader().readText())
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    /** POST with a bearer token — used for the authenticated RPC endpoints. Throws on non-2xx. */
    private fun postJsonWithToken(url: URL, body: JSONObject, token: String): JSONObject? {
        val connection = openPostConnection(url, authorized = true, token = token)
        return try {
            writeBody(connection, body)
            val code = connection.responseCode
            if (code !in 200..299) throw AuthError.serverError()
            val text = connection.inputStream.bufferedReader().readText()
            if (text.isBlank()) JSONObject() else runCatching { JSONObject(text) }.getOrDefault(JSONObject())
        } finally {
            connection.disconnect()
        }
    }
}

// MARK: - SUPPORT

/** Android analog of iOS's StubAuthService — used when SupabaseConfig.isConfigured == false. */
class StubAuthService : AuthService {
    override val currentUserId: String? = "stub-user-id"
    override val isAuthenticated: Boolean = true
    private val _authStateChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val authStateChanges: SharedFlow<Unit> = _authStateChanges.asSharedFlow()

    override suspend fun signInWithGoogle(idToken: String) {}
    override fun signOut() {}
    override suspend fun accessToken(): String? = null
    override suspend fun refreshSession() {}
    override suspend fun deleteAccount() {}
    override suspend fun updateProfile(displayName: String, avatarEmoji: String, goalMinutes: Int) {}
    override suspend fun ensureUserRow() {}
    override suspend fun syncPremiumStatus(isPremium: Boolean, isTrial: Boolean) {}
}
