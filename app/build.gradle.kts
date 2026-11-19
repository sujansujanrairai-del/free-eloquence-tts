import org.gradle.internal.os.OperatingSystem
import java.util.Properties

plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
	alias(libs.plugins.kotlin.compose)
}

val abis = (findProperty("evvdroid.abis") as String? ?: "arm64-v8a,armeabi-v7a,x86_64")
	.split(",").map { it.trim() }.filter { it.isNotEmpty() }
val rulesForm = findProperty("evvdroid.rules") as String? ?: "c"

val languages = "lang/enus lang/engb lang/dede lang/eses lang/esus lang/frfr lang/frca lang/itit"
val nativeOut = layout.buildDirectory.dir("native/jniLibs")

val buildNative by tasks.registering(Exec::class) {
	group = "build"
	description = "Cross-compiles the openevv engine and the JNI bridge for each ABI."
	val engine = rootProject.layout.projectDirectory.dir("native/openevv")
	val script = rootProject.layout.projectDirectory.file("native/build-native.sh")

	inputs.files(rootProject.fileTree("native/openevv/src"))
	inputs.files(rootProject.fileTree("native/openevv/lib"))
	inputs.file(rootProject.layout.projectDirectory.file("native/android.mk"))
	inputs.file(script)
	inputs.file(layout.projectDirectory.file("src/main/cpp/evv_jni.c"))
	inputs.property("abis", abis)
	inputs.property("rules", rulesForm)
	inputs.property("langs", languages)
	outputs.dir(nativeOut)
	val shell = if (OperatingSystem.current().isWindows) {
		val git = System.getenv("PROGRAMFILES")?.let { file("$it/Git/bin/bash.exe") }
		if (git != null && git.exists()) git.absolutePath else "bash"
	} else {
		"bash"
	}
	commandLine(shell, script.asFile.absolutePath)
	environment("ABIS", abis.joinToString(" "))
	environment("RULES", rulesForm)
	environment("LANGS", languages)
	environment("OUT", nativeOut.get().asFile.absolutePath)
	doFirst {
		if (!engine.file("Makefile").asFile.exists()) {
			throw GradleException("native/openevv has no Makefile; the engine is vendored in this repository.")
		}
	}
}

android {
	namespace = "com.blindtechnexus.freeeloquencetts"
	compileSdk = 36
	ndkVersion = "26.1.10909125"

	defaultConfig {
		applicationId = "com.blindtechnexus.freeeloquencetts"
		minSdk = 23
		targetSdk = 36
		versionCode = 5
		versionName = "1.5"
		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		ndk {
			abiFilters += abis
		}
	}

	sourceSets.getByName("androidTest") {
		kotlin.srcDirs("src/androidTest/kotlin")
	}

	sourceSets.getByName("main") {
		kotlin.srcDirs("src/main/kotlin")
		jniLibs.srcDirs(nativeOut)
	}

	signingConfigs {
		create("release") {
			storeFile = rootProject.file("release.jks")
			storePassword = "811015"
			keyAlias = "release"
			keyPassword = "811015"

			enableV3Signing = true
		}
	}

	buildTypes {
		release {
			signingConfig = signingConfigs.findByName("release")
			isMinifyEnabled = true
			isShrinkResources = true
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
		}
		debug {
			isJniDebuggable = true
		}
	}

	packaging {
		jniLibs {

			useLegacyPackaging = false
		}
	}

	splits {
		abi {
			isEnable = (findProperty("evvdroid.abiSplits") as String?)?.toBoolean() ?: false
			reset()
			include(*abis.toTypedArray())
			isUniversalApk = true
		}
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}

	kotlinOptions {
		jvmTarget = "17"
	}

	buildFeatures {
		buildConfig = true
		compose = true
	}
}

tasks.named("preBuild") {
	dependsOn(buildNative)
}

dependencies {
	implementation(libs.androidx.core.ktx)
	implementation(libs.material)
	implementation(libs.androidx.activity.compose)
	implementation(platform(libs.compose.bom))
	implementation(libs.compose.ui)
	implementation(libs.compose.material3)
	implementation(libs.compose.material.icons.core)
	implementation(libs.compose.material.icons.extended)

	implementation(libs.mlkit.language.id)

	androidTestImplementation(libs.junit)
	androidTestImplementation(libs.androidx.test.junit)
	androidTestImplementation(libs.androidx.test.runner)
	androidTestImplementation(libs.androidx.test.core)

	androidTestImplementation(platform(libs.compose.bom))
	androidTestImplementation("androidx.compose.ui:ui-test-junit4")
	debugImplementation(platform(libs.compose.bom))
	debugImplementation("androidx.compose.ui:ui-test-manifest")
}
