@file:Suppress("UnstableApiUsage")

import Config.Version.createVersion
import Config.Version.source
import Config.isRelease
import Config.lastCommitSha
import com.android.build.gradle.internal.api.BaseVariantOutputImpl

plugins {
    alias(libs.plugins.com.android.application)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.parcelize)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.serialization)
    alias(libs.plugins.com.google.devtools.ksp)
    alias(libs.plugins.com.google.gms.google.services)
    alias(libs.plugins.com.google.firebase.crashlytics)
    alias(libs.plugins.com.google.firebase.firebase.pref)
    alias(libs.plugins.compose.compiler)
}

android {
    compileSdk = property("compile.sdk")?.toString()?.toIntOrNull()

    val commitSha = if (isRelease) lastCommitSha else "b8eace8" // 方便调试

    // 先 Github Secrets 再读取环境变量，若没有则读取本地文件
    val signPwd = System.getenv("HA1_KEYSTORE_PASSWORD") ?: File(
        projectDir, "keystore/ha1_keystore_password.txt"
    ).checkIfExists()?.readText().orEmpty()

    val githubToken = System.getenv("HA1_GITHUB_TOKEN") ?: File(
        projectDir, "ha1_github_token.txt"
    ).checkIfExists()?.readText().orEmpty()

    val signConfig = if (isRelease) signingConfigs.create("release") {
        storeFile = File(projectDir, "keystore/Han1meViewerKeystore.jks").checkIfExists()
        storePassword = signPwd
        keyAlias = "ci_key"
        keyPassword = signPwd
        enableV3Signing = true
        enableV4Signing = true
    } else null

    defaultConfig {
        applicationId = "com.yenaly.han1meviewer"
        minSdk = property("min.sdk")?.toString()?.toIntOrNull()
        targetSdk = property("target.sdk")?.toString()?.toIntOrNull()
        val (code, name) = createVersion(major = 1, minor = 8, patch = 0)
        versionCode = code
        versionName = name

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "COMMIT_SHA", "\"$commitSha\"")
        buildConfigField("String", "VERSION_NAME", "\"${versionName}\"")
        buildConfigField("int", "VERSION_CODE", "$versionCode")
        buildConfigField("String", "HA1_GITHUB_TOKEN", "\"${githubToken}\"")
        buildConfigField("String", "HA1_VERSION_SOURCE", "\"${source}\"")
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signConfig
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            applicationIdSuffix = ".debug"
        }
    }
    applicationVariants.all {
        outputs.forEach { output ->
            val outputImpl = output as BaseVariantOutputImpl
            val abi = outputImpl.filters.find { it.filterType == "ABI" }?.identifier

            outputImpl.outputFileName = if (abi != null) {
                "Han1meViewer-v${defaultConfig.versionName}_$abi.apk"
            } else {
                "Han1meViewer-v${defaultConfig.versionName}_universal.apk"
            }
        }
    }
    buildFeatures {
        //noinspection DataBindingWithoutKapt
        dataBinding = true
        buildConfig = true
        // compose = true
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_21.toString()
        freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn", "-Xjvm-default=all-compatibility")
    }
    lint {
        disable += setOf("EnsureInitializerMetadata")
    }
    namespace = "com.yenaly.han1meviewer"
}

dependencies {

    implementation(project(":yenaly_libs"))

    // android related
    implementation(libs.bundles.android.base)
    implementation(libs.bundles.android.jetpack)
    implementation(libs.palette)

    // datetime
    implementation(libs.datetime)
    implementation(libs.serialization.json) // Use this instead of duplicate

    // parse
    implementation(libs.jsoup)

    // network
    implementation(libs.retrofit)
    implementation(libs.converter.serialization)

    // pic
    implementation(libs.coil)

    // mlkit
    implementation("com.google.mlkit:translate:17.0.2")
    implementation("com.jakewharton:disklrucache:2.0.2")

    // popup
    implementation(libs.xpopup)
    implementation(libs.xpopup.ext)

    // video
    implementation(libs.jiaozi.video.player)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)

    // Compose - Keep only these lines
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.runtime:runtime")

    // view
    implementation(libs.refresh.layout.kernel)
    implementation(libs.refresh.header.material)
    implementation(libs.refresh.footer.classics)
    implementation(libs.multitype)
    implementation(libs.base.recyclerview.adapter.helper4)
    implementation(libs.expandable.textview)
    implementation(libs.spannable.x)
    implementation(libs.about)
    implementation(libs.statelayout)
    implementation(libs.circular.reveal.switch)

    // firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.perf)
    implementation(libs.firebase.config)

    // mpv
    implementation(libs.mpv.lib)

    ksp(libs.room.compiler)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit)
    androidTestImplementation(libs.test.junit)
    androidTestImplementation(libs.test.espresso.core)
}
/**
 * This function is used to check if a file exists and is a file.
 */
fun File.checkIfExists(): File? = if (exists() && isFile) this else null
