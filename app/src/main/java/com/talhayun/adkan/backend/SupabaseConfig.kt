package com.talhayun.adkan.backend

import com.talhayun.adkan.BuildConfig

// [SKILL-DECL] Ported from iOS App/Backend/SupabaseConfig.swift equivalent
// (per plan/serialized-tinkering-pony-agent-a78532b0c00bea6c9.md). Values come
// from BuildConfig fields sourced out of the gitignored local.properties (see
// app/build.gradle.kts) — never hard-coded here, mirroring how iOS keeps
// SupabaseSecrets.plist out of git.
object SupabaseConfig {
    val baseURL: String = BuildConfig.SUPABASE_URL
    val anonKey: String = BuildConfig.SUPABASE_ANON_KEY
    val googleWebClientId: String = BuildConfig.GOOGLE_WEB_CLIENT_ID

    /**
     * Mirrors iOS's stub-fallback gating: until the founder fills in
     * local.properties (see plan doc), the app runs against [StubAuthService]
     * instead of crashing or hitting empty-string endpoints.
     */
    val isConfigured: Boolean
        get() = baseURL.isNotBlank() && anonKey.isNotBlank()

    val isGoogleSignInConfigured: Boolean
        get() = isConfigured && googleWebClientId.isNotBlank()
}
