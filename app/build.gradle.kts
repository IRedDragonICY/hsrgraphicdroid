plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.ireddragonicy.hsrgraphicdroid"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ireddragonicy.hsrgraphicdroid"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Language filters moved to androidResources.localeFilters (AGP 9.x)

        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    
    androidResources {
        localeFilters += listOf("en", "id", "zh", "ja", "ru")
    }

    buildFeatures {
        buildConfig = true
        aidl = false
        shaders = false
        resValues = false
        compose = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }

        resources {
            excludes += setOf(
                // META-INF
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/NOTICE.md",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/*.kotlin_module",
                "META-INF/versions/**",
                "META-INF/*.version",
                "META-INF/proguard/**",
                "META-INF/com.android.tools/**",
                "META-INF/maven/**",
                "META-INF/services/**",
                
                // Kotlin
                "DebugProbesKt.bin",
                "kotlin/**",
                "kotlin-tooling-metadata.json",
                
                // Other
                "**.properties",
                "**.proto",
                "**.bin",
                "**.txt",
                "LICENSE",
                "LICENSE.txt",
                "NOTICE",
                "NOTICE.txt",
                "androidsupportmultidexversion.txt",
                
                // Unused
                "okhttp3/internal/publicsuffix/NOTICE",
                "org/bouncycastle/**",
                "org/conscrypt/**"
            )
        }
    }

    bundle {
        abi {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        language {
            enableSplit = true
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.13.1")
    
    // Jetpack Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2026.03.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    
    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    
    // Compose Integration
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    
    // Lifecycle (runtime & viewmodel already included transitively via compose variants)
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    
    // LibSU for root access
    implementation("com.github.topjohnwu.libsu:core:5.2.2")
    
}