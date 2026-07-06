plugins {
    // Kotlin Multiplatform
    kotlin("multiplatform") version "1.9.24" apply false
    // Compose Multiplatform
    id("org.jetbrains.compose") version "1.6.11" apply false
    // Android
    id("com.android.application") version "8.4.1" apply false
    id("com.android.library") version "8.4.1" apply false
    // SQLDelight - local database that works across Android/Desktop/iOS
    id("app.cash.sqldelight") version "2.0.2" apply false
}
