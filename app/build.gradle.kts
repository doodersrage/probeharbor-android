import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.gms.google-services")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

fun localProp(key: String, default: String = ""): String =
    (localProperties.getProperty(key) ?: default).replace("\"", "\\\"")

android {
    namespace = "dev.thermaltrace.android"
    compileSdk = 36

    defaultConfig {
        // Renamed from dev.thermaltrace.android (2026-10). The Kotlin namespace
        // above stays put; only the published application ID changed.
        applicationId = "dev.probeharbor.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "1.2.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL", "\"${localProp("supabase.url")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProp("supabase.anonKey")}\"")
        buildConfigField(
            "String",
            "THERMALTRACE_BASE_URL",
            "\"${localProp("thermaltrace.baseUrl", "https://probeharbor.dev")}\"",
        )
        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"${localProp("firebase.projectId")}\"")
        buildConfigField(
            "String",
            "FIREBASE_APPLICATION_ID",
            "\"${localProp("firebase.applicationId")}\"",
        )
        buildConfigField("String", "FIREBASE_API_KEY", "\"${localProp("firebase.apiKey")}\"")
        buildConfigField(
            "String",
            "FIREBASE_GCM_SENDER_ID",
            "\"${localProp("firebase.gcmSenderId")}\"",
        )
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Include stripped native symbols in the AAB for Play Crashlytics / Vitals.
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.browser:browser:1.8.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    val supabaseBom = platform("io.github.jan-tennert.supabase:bom:3.0.3")
    implementation(supabaseBom)
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation(platform("io.ktor:ktor-bom:3.0.3"))
    implementation("io.ktor:ktor-client-android")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
