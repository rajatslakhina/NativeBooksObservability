plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.nativebooks.android.observability"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }
    buildToolsVersion = "37.0.0"

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    api("io.opentelemetry:opentelemetry-api:1.62.0")
    implementation("io.opentelemetry:opentelemetry-sdk:1.62.0")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.62.0")
    implementation("io.opentelemetry:opentelemetry-exporter-logging:1.62.0")
}
