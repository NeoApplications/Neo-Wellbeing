plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.eu.droid_ng.wellbeing.framework"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.eu.droid_ng.wellbeing.framework"
        minSdk = 29
        //noinspection OldTargetApi TODO
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        register("release") {
            if (project.hasProperty("RELEASE_KEY_ALIAS")) {
                storeFile = file(project.properties["RELEASE_STORE_FILE"].toString())
                storePassword = project.properties["RELEASE_STORE_PASSWORD"].toString()
                keyAlias = project.properties["RELEASE_KEY_ALIAS"].toString()
                keyPassword = project.properties["RELEASE_KEY_PASSWORD"].toString()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
            if (project.hasProperty("RELEASE_KEY_ALIAS")) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                logger.warn("Using debug signing configs!")
                signingConfig = signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    implementation(project(":shared"))
}