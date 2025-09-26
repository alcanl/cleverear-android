@file:Suppress("UnstableApiUsage")

import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
    id("dagger.hilt.android.plugin")
    id("kotlinx-serialization")
    id("androidx.navigation.safeargs.kotlin")
}


val properties = Properties()
properties.load(project.rootProject.file("local.properties").inputStream())

android {
    signingConfigs {
        create("release") {
            storeFile = file(properties.getProperty("KEY_PATH"))
            storePassword = properties.getProperty("STORE_PASSWORD")
            keyAlias = properties.getProperty("KEY_ALIAS")
            keyPassword = properties.getProperty("KEY_PASSWORD")
        }
    }
    namespace = "com.alcanl.cleverear"
    compileSdk = 36

    packaging.jniLibs {
        useLegacyPackaging = false
    }

    defaultConfig {
        applicationId = "com.eartechnic.Biyonix"
        minSdk = 27
        targetSdk = 36
        versionCode = 341
        versionName = "3.2.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
            abiFilters.add("armeabi")
        }

        externalNativeBuild {
            cmake {
                cppFlags.add("-frtti")
                cppFlags.add("-fexceptions")
                arguments.add("-DCMAKE_VERBOSE_MAKEFILE=ON")
            }
        }

        signingConfig = signingConfigs.getByName("release")

        buildConfigField("String", "API_KEY", "\"${properties.getProperty("API_KEY")}\"")
        buildConfigField("String", "API_KEY_HEADER", "\"${properties.getProperty("API_KEY_HEADER")}\"")
        buildConfigField("String", "API_BASE_URL", "\"${properties.getProperty("API_BASE_URL")}\"")
        buildConfigField("String", "ACCOUNT_SID", "\"${properties.getProperty("TWILIO_ACCOUNT_SID")}\"")
        buildConfigField("String", "AUTH_TOKEN", "\"${properties.getProperty("TWILIO_AUTH_TOKEN")}\"")
        buildConfigField("String", "PHONE_NUMBER", "\"${properties.getProperty("TWILIO_PHONE_NUMBER")}\"")
    }

    buildTypes {
        release {
            ndk.debugSymbolLevel =  "FULL"
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        target {
            compilerOptions {
                optIn.add("kotlin.RequiresOptIn")
            }
        }
    }
    dataBinding.enable = true
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    buildToolsVersion = "36.0.0"

    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE.txt"
        }
    }
}

dependencies {
    implementation("com.github.chnouman:AwesomeDialog:1.0.5")
    implementation("pl.bclogic:pulsator4droid:1.0.3")
    implementation("com.github.cdflynn:checkview:v1.2")
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("com.neovisionaries:nv-bluetooth:1.8")
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")
    implementation("com.github.alxrm:audiowave-progressbar:0.9.2")
    implementation("com.github.homayoonahmadi:GroupBoxLayout:1.2.0")
    implementation("org.greenrobot:eventbus:3.3.1")
    implementation("com.github.MasayukiSuda:BubbleLayout:v1.2.2")
    implementation("com.github.skydoves:powerspinner:1.2.7")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.github.razaghimahdi:Android-Loading-Dots:1.3.2")
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar", "*.so"))))
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("com.androidplot:androidplot-core:1.5.11")
    implementation("androidx.navigation:navigation-fragment-ktx:2.9.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.0")
    implementation("com.google.dagger:hilt-android-gradle-plugin:2.55")
    implementation("com.github.wisalmuhammad:Android-Battery-View:1.0.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0") { exclude(module = "okhttp") }
    implementation("com.twilio.sdk:twilio:10.9.2")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:3.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("androidx.activity:activity-ktx:1.10.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    implementation("com.google.dagger:hilt-android:2.55")
    kapt("com.google.dagger:hilt-compiler:2.55")
    kapt("androidx.hilt:hilt-compiler:1.2.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.8.1")
    implementation("androidx.compose.material:material:1.8.1")
}

kapt {
    correctErrorTypes = true
}