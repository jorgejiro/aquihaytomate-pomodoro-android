import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val keystoreProps = Properties().also { props ->
    val f = rootProject.file("keystore.properties")
    if (f.exists()) props.load(f.inputStream())
}
val hasReleaseKeystore = keystoreProps["storeFile"] != null

android {
    namespace = "com.jjrapps.aquihaytomate"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.jjrapps.aquihaytomate"
        minSdk = 31
        targetSdk = 36
        versionCode = 8
        versionName = "1.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = file(keystoreProps["storeFile"] as String)
                storePassword = keystoreProps["storePassword"] as String
                keyAlias = keystoreProps["keyAlias"] as String
                keyPassword = keystoreProps["keyPassword"] as String
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
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // AGP mete en el APK la lista de dependencias cifrada con una clave de Google:
    // solo Google puede leerla, y F-Droid rechaza los APK que la llevan.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    // MigrationTestHelper reads the exported schemas from the instrumentation assets.
    sourceSets.getByName("androidTest") {
        assets.directories.add("$projectDir/schemas")
    }
}

/**
 * El artefacto sale como `aquihaytomate-1.2.0-vc5-release.aab` en vez de `app-release.aab`.
 *
 * No es cosmética: Bebe Agua vive en la carpeta hermana de este proyecto y su bundle se llamaba
 * exactamente igual, así que al subir la 1.2.0 se cogió el de la otra app y Play la rechazó por estar
 * firmada con otra clave —las dos apps comparten cuenta y carpeta de trabajo, pero no clave de firma—.
 * Con la versión en el nombre, el fichero que hay que subir se distingue de un vistazo, y de paso queda
 * claro a qué `versionCode` corresponde un bundle guardado.
 */
base {
    archivesName = "aquihaytomate-${android.defaultConfig.versionName}-vc${android.defaultConfig.versionCode}"
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.timber)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.turbine)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
