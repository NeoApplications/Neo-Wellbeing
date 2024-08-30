import java.nio.file.Files

plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.android")
	id("com.google.devtools.ksp")
}

android {
	namespace = "org.eu.droid_ng.wellbeing"
	compileSdk = 35

	defaultConfig {
		applicationId = "org.eu.droid_ng.wellbeing"
		minSdk = 29
		//noinspection OldTargetApi TODO
		targetSdk = 33
		versionCode = 4
		versionName = "0.2.2"

		javaCompileOptions {
			annotationProcessorOptions {
				arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
			}
		}
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
		debug {
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

ksp {
	arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
	implementation(project(":shared"))
	val roomVersion = "2.4.0-alpha05" // Android 13 (https://cs.android.com/android/platform/superproject/+/android-13.0.0_r31:prebuilts/sdk/current/androidx/m2repository/androidx/room/room-runtime/;bpv=1)
	//noinspection GradleDependency
	implementation("androidx.room:room-runtime:$roomVersion")
	//noinspection GradleDependency
	annotationProcessor("androidx.room:room-compiler:$roomVersion")
	//noinspection GradleDependency
	ksp("androidx.room:room-compiler:$roomVersion")

	implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")

	implementation("androidx.recyclerview:recyclerview:1.3.2")
	implementation("androidx.constraintlayout:constraintlayout:2.1.4")
	implementation("androidx.preference:preference-ktx:1.2.1")
	implementation("androidx.appcompat:appcompat:1.7.0")
	implementation("com.google.android.material:material:1.12.0")

	implementation("com.github.AppDevNext:AndroidChart:3.1.0.15")
}

val outDir = rootProject.layout.buildDirectory.asFile.get()
val magiskDir = File("$outDir/magisk_module")
val zipName = "NeoWellbeing-${android.defaultConfig.versionName}.zip"
val zipFile = File(outDir, zipName)
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

tasks.register("assembleMagiskModule", Task::class) {
	val root = rootDir
	val magisk = magiskDir
	var modulePropText = ""
	val appApk = file("$root/app/build/outputs/apk/release/app-release.apk")
	val frameworkApk = file("$root/framework/build/outputs/apk/release/framework-release.apk")
	magiskModuleProp.forEach { (k, v) -> modulePropText += "$k=$v\n" }

	dependsOn(":app:assembleRelease")
	dependsOn(":framework:assembleRelease")
	doLast {
		magisk.deleteRecursively()
		magisk.mkdirs()
		File("$magisk/module.prop").writeText(modulePropText)
		File("${magisk.path}/system/priv-app/NeoWellbeing").mkdirs()
		Files.copy(appApk.toPath(),
				File("${magisk.path}/system/priv-app/NeoWellbeing/NeoWellbeing.apk").toPath())
		File("${magisk.path}/system/priv-app/NeoWellbeingFramework").mkdirs()
		Files.copy(frameworkApk.toPath(),
				File("${magisk.path}/system/priv-app/NeoWellbeingFramework/NeoWellbeingFramework.apk").toPath())
		File("${magisk.path}/system/product/overlay/NeoWellbeingOverlay").mkdirs()
		Files.copy(File("$root/NeoWellbeingOverlay/overlay.apk").toPath(),
				File("${magisk.path}/system/product/overlay/NeoWellbeingOverlay/NeoWellbeingOverlay.apk").toPath())
		File("${magisk.path}/system/etc/permissions").mkdirs()
		Files.copy(File("$root/app/src/main/privapp-permissions-wellbeing.xml").toPath(),
				File("${magisk.path}/system/etc/permissions/privapp-permissions-wellbeing.xml").toPath())
		File("${magisk.path}/META-INF/com/google/android").mkdirs()
		File("${magisk.path}/META-INF/com/google/android/updater-script").writeText("#MAGISK")
		Files.copy(File("$root/app/update-binary").toPath(),
				File("${magisk.path}/META-INF/com/google/android/update-binary").toPath())
		Files.copy(File("$root/app/customize.sh").toPath(),
				File("${magisk.path}/customize.sh").toPath())
	}
}

tasks.register("zipMagiskModule", Zip::class) {
	from(magiskDir)
	archiveFileName = zipName
	destinationDirectory = outDir
	dependsOn(":app:assembleMagiskModule")
}

tasks.register("pushMagiskModule", Exec::class) {
	commandLine("adb", "push", zipFile.absolutePath, "/data/local/tmp/$zipName")
	dependsOn(":app:zipMagiskModule")
}

tasks.register("testMagiskModule", Exec::class) {
	setIgnoreExitValue(true)
	commandLine("adb", "shell", "su", "-c",
			"magisk --install-module /data/local/tmp/" + zipName +
					" && (/system/bin/svc power reboot || /system/bin/reboot)")
	dependsOn(":app:pushMagiskModule")
}
