import java.nio.file.Files

plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.android")
}

android {
	namespace = "org.eu.droid_ng.wellbeing"
	compileSdk = 34

	defaultConfig {
		applicationId = "org.eu.droid_ng.wellbeing"
		minSdk = 29
		//noinspection OldTargetApi TODO
		targetSdk = 33
		versionCode = 4
		versionName = "0.2.2"
	}

	buildFeatures {
		aidl = true
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
		named("release") {
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

	sourceSets {
		getByName("main") {
			java.srcDirs("src/main/java_magisk")
			kotlin.srcDirs("src/main/java_magisk")
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

	implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")

	implementation("androidx.recyclerview:recyclerview:1.3.2")
	implementation("androidx.constraintlayout:constraintlayout:2.1.4")
	implementation("androidx.preference:preference-ktx:1.2.1")
	implementation("androidx.appcompat:appcompat:1.6.1")
	implementation("com.google.android.material:material:1.11.0")

	implementation("com.github.AppDevNext:AndroidChart:3.1.0.15")
}

val magiskModuleProp = mapOf(
		"id" to "neo_wellbeing",
		"name" to "Neo Wellbeing systemless",
		"version" to android.defaultConfig.versionName,
		"versionCode" to android.defaultConfig.versionCode,
		"minApi" to android.defaultConfig.minSdk,
		"support" to "https://github.com/NeoApplications/Neo-Wellbeing",
		"config" to "org.eu.droid_ng.wellbeing",
		"author" to "nift4",
		"description" to "Neo Wellbeing is an open source reimplementation of Wellbeing"
)

val outDir = rootProject.layout.buildDirectory
val magiskDir = file("$outDir/magisk_module")
val zipName = "NeoWellbeing-${android.defaultConfig.versionName}.zip"
val zipFile = File(outDir.asFile.get(), zipName)

tasks.register("assembleMagiskModule", Task::class) {
	dependsOn(":app:assembleRelease")
	dependsOn(":framework:assembleRelease")
	doLast {
		delete(magiskDir)
		magiskDir.mkdirs()
		var modulePropText = ""
		magiskModuleProp.forEach { k, v -> modulePropText += "$k=$v\n" }
		file("$magiskDir/module.prop").writeText(modulePropText)
		file("$magiskDir.path/system/priv-app/NeoWellbeing").mkdirs()
		Files.copy(file("$rootDir/app/build/outputs/apk/release/app-release.apk").toPath(),
				file("${magiskDir.path}/system/priv-app/NeoWellbeing/NeoWellbeing.apk").toPath())
		file("$magiskDir.path/system/priv-app/NeoWellbeingFramework").mkdirs()
		Files.copy(file("$rootDir/framework/build/outputs/apk/release/framework-release.apk").toPath(),
				file("${magiskDir.path}/system/priv-app/NeoWellbeingFramework/NeoWellbeingFramework.apk").toPath())
		file("$magiskDir.path/system/product/overlay/NeoWellbeingOverlay").mkdirs()
		Files.copy(file("$rootDir/NeoWellbeingOverlay/overlay.apk").toPath(),
				file("${magiskDir.path}/system/product/overlay/NeoWellbeingOverlay/NeoWellbeingOverlay.apk").toPath())
		file("$magiskDir.path/system/etc/permissions").mkdirs()
		Files.copy(file("$rootDir/app/src/main/privapp-permissions-wellbeing.xml").toPath(),
				file("${magiskDir.path}/system/etc/permissions/privapp-permissions-wellbeing.xml").toPath())
		file("$magiskDir.path/META-INF/com/google/android").mkdirs()
		file("$magiskDir.path/META-INF/com/google/android/updater-script").writeText("#MAGISK")
		Files.copy(file("$rootDir/app/update-binary").toPath(),
				file("$magiskDir.path/META-INF/com/google/android/update-binary").toPath())
		Files.copy(file("$rootDir/app/customize.sh").toPath(),
				file("$magiskDir.path/customize.sh").toPath())
	}
}

tasks.register("zipMagiskModule", Zip::class) {
	from(magiskDir)
	archiveFileName = zipName
	destinationDirectory = outDir
	dependsOn(":app:assembleMagiskModule")
}

tasks.register("pushMagiskModule", Exec::class) {
	commandLine("adb", "push", zipFile.absolutePath, "/sdcard/Documents/$zipName")
	dependsOn(":app:zipMagiskModule")
}

tasks.register("testMagiskModule", Exec::class) {
	commandLine("adb", "shell", "su", "-c",
			"magisk --install-module /sdcard/Documents/" + zipName +
					" && (/system/bin/svc power reboot || /system/bin/reboot)")
	dependsOn(":app:pushMagiskModule")
}
