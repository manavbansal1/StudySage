import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    alias(libs.plugins.kotlin.serialization)
}


// Load .env file
val envFile = rootProject.file(".env")
val envProperties = Properties()
if (envFile.exists()) {
    FileInputStream(envFile).use { stream ->
        envProperties.load(stream)
    }
    println("✓ .env file loaded successfully")
    println("✓ GEMINI_API_KEY: ${if (envProperties.getProperty("GEMINI_API_KEY")?.isNotEmpty() == true) "Found" else "Missing"}")
    println("✓ CLOUDINARY_CLOUD_NAME: ${if (envProperties.getProperty("CLOUDINARY_CLOUD_NAME")?.isNotEmpty() == true) "Found" else "Missing"}")
} else {
    println("⚠ .env file not found at: ${envFile.absolutePath}")
}

fun getEnvOrDefault(key: String, default: String = ""): String {
    val value = envProperties.getProperty(key)?.trim() ?: System.getenv(key)?.trim() ?: default
    if (value.isEmpty() && default.isEmpty()) {
        println("⚠ Warning: $key is empty or not found")
    }
    return value
}

android {
    namespace = "com.group_7.studysage"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.group_7.studysage"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "GEMINI_API_KEY", "\"${getEnvOrDefault("GEMINI_API_KEY")}\"")
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"${getEnvOrDefault("CLOUDINARY_CLOUD_NAME")}\"")
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"${getEnvOrDefault("CLOUDINARY_UPLOAD_PRESET")}\"")
        buildConfigField("String", "RESEND_API_KEY", "\"${getEnvOrDefault("RESEND_API_KEY")}\"")
        buildConfigField("String", "GOOGLE_CLOUD_TTS_API_KEY", "\"${getEnvOrDefault("GOOGLE_CLOUD_TTS_API_KEY")}\"")
        buildConfigField("String", "CLOUD_RUN_URL", "\"${getEnvOrDefault("CLOUD_RUN_URL")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Jetpack Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Activity Compose
    implementation("androidx.activity:activity-compose:1.9.3")

    // Accompanist - Pull-to-refresh
    implementation("com.google.accompanist:accompanist-swiperefresh:0.32.0")

    // ViewModel Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    // Firebase AI (Vertex AI in Firebase)
    implementation("com.google.firebase:firebase-vertexai:16.0.2")

    // Gemini AI (Google AI Client SDK) - Keep this as fallback
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // Coil for Compose (image loading)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // OkHttp for Cloudinary uploads
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")


    // Retrofit (if needed for other APIs)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // PDF processing
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    // Gson for JSON parsing (Cloudinary response)
    implementation("com.google.code.gson:gson:2.10.1")

    // Navigation (keeping for compatibility)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Accompanist for permissions
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // WorkManager for background tasks
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
