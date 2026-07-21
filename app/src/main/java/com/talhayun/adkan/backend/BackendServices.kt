package com.talhayun.adkan.backend

import android.content.Context

// [SKILL-DECL] Android analog of iOS App/Backend/ServiceContainer.swift (per
// plan/serialized-tinkering-pony-agent-a78532b0c00bea6c9.md). No Hilt since
// none exists yet in this codebase — a tiny lazy singleton is proportionate to
// the current single-module, no-DI scope.
object BackendServices {
    @Volatile
    private var authInstance: AuthService? = null

    fun auth(context: Context): AuthService {
        return authInstance ?: synchronized(this) {
            authInstance ?: buildAuthService(context).also { authInstance = it }
        }
    }

    private fun buildAuthService(context: Context): AuthService {
        return if (SupabaseConfig.isConfigured) {
            SupabaseAuthService(
                appContext = context.applicationContext,
                baseURL = SupabaseConfig.baseURL,
                apiKey = SupabaseConfig.anonKey,
            )
        } else {
            StubAuthService()
        }
    }
}
