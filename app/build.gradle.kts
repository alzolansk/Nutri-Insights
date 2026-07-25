import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// --- FatSecret credentials ---------------------------------------------------
// Values are read from local.properties (which is git-ignored) so that real
// secrets never end up in version control or in the compiled Kotlin sources.
// Fill in the three FATSECRET_* keys in local.properties. See README.md.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun secret(name: String, default: String = ""): String =
    (localProperties.getProperty(name) ?: System.getenv(name) ?: default)

android {
    namespace = "com.example.widgetfatsecret"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.widgetfatsecret"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Exposed to the app via BuildConfig.* — never hard-coded in Kotlin.
        buildConfigField(
            "String",
            "FATSECRET_CONSUMER_KEY",
            "\"${secret("FATSECRET_CONSUMER_KEY")}\""
        )
        buildConfigField(
            "String",
            "FATSECRET_CONSUMER_SECRET",
            "\"${secret("FATSECRET_CONSUMER_SECRET")}\""
        )
        buildConfigField(
            "String",
            "FATSECRET_CALLBACK_URL",
            "\"${secret("FATSECRET_CALLBACK_URL", "widgetfatsecret://oauth-callback")}\""
        )

        // The deep-link scheme/host used by the OAuth callback intent-filter is
        // derived from FATSECRET_CALLBACK_URL so the manifest and code stay in sync.
        val callback = secret("FATSECRET_CALLBACK_URL", "widgetfatsecret://oauth-callback")
        val scheme = callback.substringBefore("://", "widgetfatsecret")
        val host = callback.substringAfter("://", "oauth-callback").substringBefore("/")
        manifestPlaceholders["fatSecretCallbackScheme"] = scheme
        manifestPlaceholders["fatSecretCallbackHost"] = host
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // FatSecret integration
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
