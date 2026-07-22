import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    iosArm64()
    iosSimulatorArm64()

    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "SharedKit"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation("io.ktor:ktor-client-core:3.5.1")
            implementation("io.ktor:ktor-client-mock:3.5.1")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }

        androidMain.dependencies {
            implementation("io.opentelemetry:opentelemetry-api:1.62.0")
        }
    }
}

android {
    namespace = "com.example.nativebooks.shared"
    compileSdk = 35
    buildToolsVersion = "37.0.0"

    defaultConfig {
        minSdk = 24
    }
}
