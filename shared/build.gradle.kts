plugins {
	id("com.android.library")
	id("org.jetbrains.kotlin.android")
	id("com.google.devtools.ksp")
}

android {
	namespace = "org.eu.droid_ng.wellbeing.shared"
	compileSdk = 34

	defaultConfig {
		minSdk = 29
		lint {
			targetSdk = 33
		}

		consumerProguardFiles("consumer-rules.pro")

		javaCompileOptions {
			annotationProcessorOptions {
				arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
			}
		}
	}

	buildFeatures {
		aidl = true
	}

	sourceSets {
		getByName("main") {
			java.srcDir("src/main/java_magisk")
			kotlin.srcDir("src/main/java_magisk")
		}
	}

	buildTypes {
		named("release") {
			isMinifyEnabled = true
			setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
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
	implementation("androidx.annotation:annotation:1.7.1")
	val roomVersion = "2.4.0-alpha05" // Android 13 (https://cs.android.com/android/platform/superproject/+/android-13.0.0_r31:prebuilts/sdk/current/androidx/m2repository/androidx/room/room-runtime/;bpv=1)
	//noinspection GradleDependency
	implementation("androidx.room:room-runtime:$roomVersion")
	//noinspection GradleDependency
	annotationProcessor("androidx.room:room-compiler:$roomVersion")
	//noinspection GradleDependency
	ksp("androidx.room:room-compiler:$roomVersion")
	// For gradle builds only
	implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
}