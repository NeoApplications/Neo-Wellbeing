// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
	id("com.android.application") version "8.4.0-rc01" apply false
	id("com.android.library") version "8.4.0-rc01" apply false
	id("org.jetbrains.kotlin.android")  version "2.0.0-RC1" apply false
	id("com.google.devtools.ksp") version "2.0.0-RC1-1.0.20" apply false
}

tasks.withType(JavaCompile::class.java) {
	options.compilerArgs.add("-Xlint:all")
}

val dir = rootProject.layout.buildDirectory.get().asFile
tasks.register("clean", type = Delete::class) {
	delete(dir)
}