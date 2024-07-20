// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
	val agpVersion = "8.5.0"
	id("com.android.application") version agpVersion apply false
	id("com.android.library") version agpVersion apply false
	val kotlinVersion = "2.0.0"
	id("org.jetbrains.kotlin.android")  version kotlinVersion apply false
	id("com.google.devtools.ksp") version "$kotlinVersion-1.0.22" apply false
}

tasks.withType(JavaCompile::class.java) {
	options.compilerArgs.add("-Xlint:all")
}

val dir = rootProject.layout.buildDirectory.get().asFile
tasks.register("clean", type = Delete::class) {
	delete(dir)
}