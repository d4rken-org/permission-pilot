import com.android.build.gradle.LibraryExtension
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import java.time.Instant

object ProjectConfig {
    const val minSdk = 21
    const val compileSdk = 31
    const val targetSdk = 31

    object Version {
        const val major = 0
        const val minor = 0
        const val patch = 6
        const val build = 0

        const val name = "${major}.${minor}.${patch}"
        const val fullName = "${name}.${build}"
        const val code = major * 1000000 + minor * 10000 + patch * 100 + build
    }
}

fun lastCommitHash(): String = Runtime.getRuntime().exec("git rev-parse --short HEAD").let { process ->
    process.waitFor()
    val output = process.inputStream.use { input ->
        input.bufferedReader().use {
            it.readText()
        }
    }
    process.destroy()
    output.trim()
}

fun buildTime(): Instant = Instant.now()

/**
 * Configures the [kotlinOptions][org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions] extension.
 */
private fun LibraryExtension.kotlinOptions(configure: Action<org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions>): Unit =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("kotlinOptions", configure)

fun LibraryExtension.setupLibraryDefaults() {
    compileSdk = ProjectConfig.compileSdk

    defaultConfig {
        minSdk = ProjectConfig.minSdk
        targetSdk = ProjectConfig.targetSdk

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xuse-experimental=kotlinx.coroutines.FlowPreview",
            "-Xuse-experimental=kotlin.time.ExperimentalTime",
            "-Xopt-in=kotlin.RequiresOptIn"
        )
    }

    packagingOptions {
        resources.excludes += "DebugProbesKt.bin"
    }
}