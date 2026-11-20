plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.isqu.ai"
    compileSdk = 37 // Wajib minimal 37 (sama/lebih tinggi dari targetSdk) untuk AndroidX 2026
    ndkVersion = "30.0.16248370"

    defaultConfig {
        applicationId = "com.isqu.ai"
        minSdk = 27
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        ndk {
            // Hapus "armeabi-v7a" jika fokus ke AI modern/Vulkan agar ukuran APK jauh lebih kecil
            abiFilters += listOf("arm64-v8a")
        }

        externalNativeBuild {
            cmake {
                cppFlags += listOf(
                    "-std=c++23",
                    "-DVK_USE_PLATFORM_ANDROID_KHR"
                )
                arguments += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON"
                )
            }
        }
    }

    buildTypes {
        debug {
            externalNativeBuild {
                cmake {
                    cppFlags += "-O1"
                }
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            externalNativeBuild {
                cmake {
                    cppFlags += listOf("-O3", "-flto")
                }
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "4.2.1"
        }
    }

    // Pengganti aaptOptions yang sudah dihapus di AGP modern
    androidResources {
        noCompress += listOf("tflite", "lite", "bin")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

// Pengganti kotlinOptions { jvmTarget = ... } yang deprecated di Kotlin 2.x
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation(platform("androidx.compose:compose-bom:2026.11.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // LiteRT (Pengganti resmi TensorFlow Lite di Google AI Edge)
    implementation("com.google.ai.edge.litert:litert:2.3.0")
    implementation("com.google.ai.edge.litert:litert-gpu:2.3.0")
    implementation("com.google.ai.edge.litert:litert-support:2.3.0")

    // Coroutines untuk inferensi non-blocking & notifikasi real-time
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
}
