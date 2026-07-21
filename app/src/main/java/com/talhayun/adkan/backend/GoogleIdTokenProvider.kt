package com.talhayun.adkan.backend

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

// [SKILL-DECL] Confirmed via developer.android.com Credential Manager docs and
// Google's googleid reference (per
// plan/serialized-tinkering-pony-agent-a78532b0c00bea6c9.md): the current,
// non-deprecated Google Sign-In path is androidx.credentials (CredentialManager),
// NOT the old GoogleSignInClient/GoogleSignInOptions. The documented cast path
// is CustomCredential + GoogleIdTokenCredential.createFrom(credential.data) —
// NOT a direct `is GoogleIdTokenCredential` check, which shows up in some blog
// posts but is not the officially documented pattern.
object GoogleIdTokenProvider {

    /**
     * Launches the Credential Manager Google Sign-In flow and returns the raw
     * Google ID token. Throws on cancel/failure so the caller can show an error;
     * callers should wrap this in their own try/catch for UI feedback.
     */
    suspend fun requestIdToken(context: Context, webClientId: String): String {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(context)
        val result = credentialManager.getCredential(context, request)

        val credential = result.credential
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw AuthError.missingToken()
        }

        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        return googleIdTokenCredential.idToken
    }
}
