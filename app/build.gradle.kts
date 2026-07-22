import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// [SKILL-DECL] android-development skill (BuildConfig-from-local.properties
// pattern) + plan/serialized-tinkering-pony-agent-a78532b0c00bea6c9.md, mirroring
// how iOS keeps SupabaseSecrets.plist out of git and templates through
// SupabaseConfig.swift. local.properties is already gitignored (verified).
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

fun localProp(key: String): String = localProperties.getProperty(key, "")

android {
    namespace = "com.talhayun.adkan"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.talhayun.adkan"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"

        buildConfigField("String", "SUPABASE_URL", "\"${localProp("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProp("SUPABASE_ANON_KEY")}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${localProp("GOOGLE_WEB_CLIENT_ID")}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    // Real vector icons for the bottom tab bar (Home/Friends/Groups/Blocking/
    // Settings) — replaces emoji-as-navigation-icons, a real anti-pattern per
    // the ui-ux-pro-max audit (emoji are font-dependent, not tintable to
    // match selected/unselected nav states, and don't scale like SVG/vector
    // assets). Version managed by the compose-bom already declared above.
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Google Sign-In via Credential Manager (NOT the deprecated GoogleSignInClient).
    // Pinned to 1.3.0 (stable, Oct 2024) rather than the newer 1.5.0/1.6.0 releases —
    // those ship Kotlin metadata that this project's Kotlin 1.9.24 compiler can't read
    // ("Class 'kotlin.Unit' was compiled with an incompatible version of Kotlin").
    // Bumping the whole Kotlin/Compose-compiler toolchain to 2.x is the longer-term fix
    // but is a bigger, riskier change to make without a working local build loop.
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Encrypted token storage. Deprecated as of 1.1.0-alpha07 (Google's
    // long-term direction is DataStore + raw Tink) but still shipped/functional;
    // see plan doc for the explicit trade-off note.
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Test-only — plain JUnit4, no Robolectric/instrumented tests (see plan's Global Constraints).
    testImplementation("junit:junit:4.13.2")
}
