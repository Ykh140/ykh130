plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.bayan.app.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bayan.app.android"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-mvp"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }
}

dependencies {
    implementation(project(":shared"))

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui:1.6.8")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.8")
    implementation("androidx.compose.material:material-icons-extended:1.6.8")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
}
