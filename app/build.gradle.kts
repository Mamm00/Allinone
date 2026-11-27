import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val keystorePropertiesFile = rootProject.file("key.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.hyperlauncher.ai"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.hyperlauncher.ai"
        minSdk = 28
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.1"
        buildConfigField("boolean", "ENABLE_IMPORT", (providers.gradleProperty("enableImport").orNull ?: "true"))
        buildConfigField("boolean", "ENABLE_FEED", (providers.gradleProperty("enableFeed").orNull ?: "true"))
        buildConfigField("boolean", "ENABLE_GLASS", (providers.gradleProperty("enableGlass").orNull ?: "true"))

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    flavorDimensions += "edition"
    productFlavors {
        create("offline") {
            dimension = "edition"
            applicationIdSuffix = ""
            versionNameSuffix = ""
            // Offline flavor: NO INTERNET permission. Enforced via manifest merger.
        }
        create("network") {
            dimension = "edition"
            applicationIdSuffix = ".network"
            versionNameSuffix = "-network"
            // Network flavor: adds INTERNET via src/network/AndroidManifest.xml
        }
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                keyAlias = keystoreProperties["keyAlias"] as String?
                keyPassword = keystoreProperties["keyPassword"] as String?
                storeFile = file(keystoreProperties["storeFile"] as String? ?: "")
                storePassword = keystoreProperties["storePassword"] as String?
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":feature-glass"))
    implementation(project(":feature-feed"))
    implementation(project(":feature-import"))
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("androidx.window:window:1.4.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.sqlcipher)
    implementation(libs.security.crypto)
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)

    // Network flavor only (use add() — flavor accessor unresolved in AGP 8.13 Kotlin DSL)
    add("networkImplementation", libs.okhttp)
    add("networkImplementation", libs.okhttp.logging)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockito.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation(libs.androidx.compose.ui.tooling)
}
