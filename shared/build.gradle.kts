plugins {
	id("com.android.library")
	id("org.jetbrains.kotlin.android")
}

android {
	namespace = "org.eu.droid_ng.wellbeing.shared"
	compileSdk = 35

	defaultConfig {
		minSdk = 29
		lint {
			targetSdk = 35
		}

		consumerProguardFiles("consumer-rules.pro")
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

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}

	kotlin {
		jvmToolchain(17)
	}
}

dependencies {
	implementation("androidx.annotation:annotation:1.8.2")
	// For gradle builds only
	implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
}