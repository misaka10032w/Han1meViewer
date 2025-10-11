@file:Suppress("UnstableApiUsage")

import Config.Version.createVersion
import Config.Version.source
import Config.isRelease
import Config.lastCommitSha
import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
    id("com.mikepenz.aboutlibraries.plugin") version "12.2.4"
    id("com.github.ben-manes.versions") version "0.52.0"
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
        val (code, name) = createVersion(major = 0, minor = 20, patch = 0)
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
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEYSTORE_PASSWORD")
        }
    }

    splits {
        abi {
            isEnable = (gradle.startParameter.taskRequests.toString().contains("Release"))
            reset()
            include("arm64-v8a")
            isUniversalApk = false
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            manifestPlaceholders.put("appIcon", "@mipmap/ic_launcher")

            applicationVariants.all variant@{
                this@variant.outputs.all output@{
                    val output = this@output as BaseVariantOutputImpl
                    val versionName = defaultConfig.versionName
                    output.outputFileName = "Han1meViewer-v${versionName}.apk"
                }
            }
        }

        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            applicationIdSuffix = ".debug"
            manifestPlaceholders.put("appIcon", "@mipmap/ic_launcher_debug")
        }
    }
    buildFeatures {
        //noinspection DataBindingWithoutKapt
        dataBinding = true
        buildConfig = true
        viewBinding = true
        compose  =  true
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        compilerOptions {
            jvmTarget.value(JvmTarget.JVM_21)
            freeCompilerArgs.addAll(
                "-opt-in=kotlin.RequiresOptIn",
                "-Xjvm-default=all-compatibility"
            )
        }
    }
    lint {
        disable += setOf("EnsureInitializerMetadata")
    }
    namespace = "com.yenaly.han1meviewer"
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.androidx.window)
    implementation(libs.androidx.window.java)
    implementation(project(":yenaly_libs"))
    implementation(libs.aboutlibraries.core)
    implementation(libs.aboutlibraries)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.swiperefreshlayout)
    // android related

    implementation(libs.bundles.android.base)
    implementation(libs.bundles.android.jetpack)
    implementation(libs.palette)
    implementation(libs.material)
    //compose
    implementation(platform(libs.compose.compose.bom))
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui.ui.tooling.preview)
    implementation(libs.androidx.ui)
    androidTestImplementation(platform(libs.compose.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.compose.ui.ui.tooling)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    //for QRCODE scanner
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.barcode.scanning)

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

    implementation(libs.xpopup){
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-android-extensions-runtime")
    }
    implementation(libs.xpopup.ext){
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-android-extensions-runtime")
    }

    // video

    implementation(libs.jiaozi.video.player)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.mpv.lib)

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
    implementation(libs.drawerlayout)

    // firebase

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.perf)
    implementation(libs.firebase.config)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.firebase.database)
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
