plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("app.cash.sqldelight")
}

// إصدارات مكتبات Supabase/Ktor - موحّدة هون عشان يسهل تحديثها
val supabaseVersion = "3.6.0"
val ktorVersion = "3.3.0"

kotlin {
    androidTarget()

    // Desktop target added later (Phase 2 - PC support)
    // jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("app.cash.sqldelight:runtime:2.0.2")
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.2")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")

                // Supabase - مصادقة سحابية + قاعدة بيانات عن بعد (Postgres) للمزامنة بين الأجهزة
                implementation(platform("io.github.jan-tennert.supabase:bom:$supabaseVersion"))
                implementation("io.github.jan-tennert.supabase:auth-kt")
                implementation("io.github.jan-tennert.supabase:postgrest-kt")
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("app.cash.sqldelight:android-driver:2.0.2")
                implementation("io.ktor:ktor-client-android:$ktorVersion")
            }
        }
    }
}

android {
    namespace = "com.bayan.app.shared"
    compileSdk = 34
    defaultConfig {
        // Supabase-kt يتطلب حد أدنى minSdk 26 (auth-kt يستخدم java.time)
        minSdk = 26
    }
}

sqldelight {
    databases {
        create("BayanDatabase") {
            packageName.set("com.bayan.app.db")
        }
    }
}
