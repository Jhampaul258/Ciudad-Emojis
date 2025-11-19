import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)

    kotlin("plugin.serialization")

    id("com.google.gms.google-services")
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm("desktop")

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
            implementation("com.google.android.gms:play-services-auth:21.2.0")
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(project.dependencies.platform(libs.android.firebase.bom))
            implementation(libs.ktor.client.android) // AÑADIDO

        }
        commonMain.dependencies {
//            implementation("cafe.adriel.voyager:voyager-screenmodel:1.1.0-beta01")

            implementation("org.apache.poi:poi-ooxml:5.2.3")
            // Core de Compose
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            // SOLO Material 3
            implementation(compose.material3)

            // Iconos extendidos (que funcionan con Material 3)
            implementation(libs.androidx.material.icons.extended)

            // Navegación con Voyager
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.transitions)
            implementation(libs.voyager.tab.navigator)

            // Firebase y Coroutines
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.gitlive.firebase.firestore)
            implementation(libs.gitlive.firebase.auth)
            //para vercel
            implementation(libs.coil.compose)
            implementation(libs.coil.network.okhttp)
            implementation("io.ktor:ktor-network:2.3.5")
            implementation("io.ktor:ktor-client-core:2.3.11")

            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)




        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation("org.apache.pdfbox:pdfbox:2.0.27")
        }
    }
}

android {
    namespace = "com.ciudademojis.firestore"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "com.ciudademojis.firestore"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
    }
    dependencies {
        debugImplementation(compose.uiTooling)
        coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.ciudademojis.firestore"
            packageVersion = "1.0.0"
        }
    }
}
