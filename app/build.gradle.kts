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
    // alias(libs.plugins.compose.compiler)
}

android {
    compileSdk = property("compile.sdk")?.toString()?.toIntOrNull()

    val commitSha = if (isRelease) lastCommitSha else "b8eace8" // 方便调试

    // 先 Github Secrets 再读取环境变量，若没有则读取本地文件

    val githubToken = System.getenv("HA_GITHUB_TOKEN") ?: File(
        projectDir, "ha1_github_token.txt"
    ).checkIfExists()?.readText().orEmpty()


    defaultConfig {
        applicationId = "com.yenaly.han1meviewer"
        minSdk = property("min.sdk")?.toString()?.toIntOrNull()
        targetSdk = property("target.sdk")?.toString()?.toIntOrNull()
        val (code, name) = createVersion(major = 0, minor = 15, patch = 5)
        versionCode = code
        versionName = name

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "COMMIT_SHA", "\"$commitSha\"")
        buildConfigField("String", "VERSION_NAME", "\"${versionName}\"")
        buildConfigField("int", "VERSION_CODE", "$versionCode")
        buildConfigField("String", "HA_GITHUB_TOKEN", "\"${githubToken}\"")
        buildConfigField("String", "VERSION_SOURCE", "\"${source}\"")

        buildConfigField("int", "SEARCH_YEAR_RANGE_END", "${Config.thisYear}")
    }
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("HOME") + "/.android/keystore.jks")
            storePassword = System.getenv("YOUR_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("YOUR_KEYSTORE_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            applicationVariants.all variant@{
                this@variant.outputs.all output@{
                    val output = this@output as BaseVariantOutputImpl
                    output.outputFileName = "Han1meViewer-v${defaultConfig.versionName}.apk"
                }
            }
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            applicationIdSuffix = ".debug"
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
    implementation(libs.androidx.appcompat.v161)
    implementation(libs.androidx.window)
    implementation(libs.androidx.window.java)

    implementation(project(":yenaly_libs"))

    // android related

    implementation(libs.bundles.android.base)
    implementation(libs.bundles.android.jetpack)
    implementation(libs.palette)

//    implementation(platform(libs.compose.compose.bom))
//    androidTestImplementation(platform(libs.compose.compose.bom))
//    implementation(libs.compose.material3)
//    implementation(libs.androidx.activity.compose)
//    implementation(libs.compose.ui.graphics)
//    implementation(libs.compose.ui.ui.tooling.preview)
//    debugImplementation(libs.compose.ui.ui.tooling)

    // datetime

    implementation(libs.datetime)

    // parse

    implementation(libs.serialization.json)
    implementation(libs.jsoup)

    // network

    implementation(libs.retrofit)
    implementation(libs.converter.serialization)

    // pic

    implementation(libs.coil)

    // popup

    implementation(libs.xpopup)
    implementation(libs.xpopup.ext)

    // video

    implementation(libs.jiaozi.video.player)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)

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

    ksp(libs.room.compiler)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit)

    androidTestImplementation(libs.test.junit)
    androidTestImplementation(libs.test.espresso.core)

    // debugImplementation(libs.leak.canary)
}

/**
 * This function is used to check if a file exists and is a file.
 */
fun File.checkIfExists(): File? = if (exists() && isFile) this else null
