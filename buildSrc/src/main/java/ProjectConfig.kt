import com.android.build.api.dsl.Packaging
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import java.io.File
import java.io.FileInputStream
import java.util.Properties

object ProjectConfig {
    const val minSdk = 21
    const val compileSdk = 35
    const val targetSdk = 35

    object Version {
        val versionProperties = Properties().apply {
            load(FileInputStream(File("version.properties")))
        }
        val major = versionProperties.getProperty("project.versioning.major").toInt()
        val minor = versionProperties.getProperty("project.versioning.minor").toInt()
        val patch = versionProperties.getProperty("project.versioning.patch").toInt()
        val build = versionProperties.getProperty("project.versioning.build").toInt()

        val name = "${major}.${minor}.${patch}-rc${build}"
        val code = major * 10000000 + minor * 100000 + patch * 1000 + build * 10
    }
}

fun lastCommitHash(): String = ProcessBuilder("git", "rev-parse", "--short", "HEAD").start().let { process ->
    process.waitFor()
    val output = process.inputStream.use { input ->
        input.bufferedReader().use {
            it.readText()
        }
    }
    process.destroy()
    output.trim()
}

/**
 * Configures the [kotlinOptions][org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions] extension.
 */
private fun LibraryExtension.kotlinOptions(configure: Action<org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions>): Unit =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("kotlinOptions", configure)

fun LibraryExtension.setupLibraryDefaults() {
    compileSdk = ProjectConfig.compileSdk

    defaultConfig {
        minSdk = ProjectConfig.minSdk

        testOptions {
            unitTests {
                isIncludeAndroidResources = true
            }
        }
    }

    lint {
        targetSdk = ProjectConfig.targetSdk
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-opt-in=kotlin.time.ExperimentalTime",
            "-opt-in=kotlin.RequiresOptIn"
        )
    }

    packaging {
        resources.excludes += "DebugProbesKt.bin"
    }
}

fun com.android.build.api.dsl.SigningConfig.setupCredentials(
    signingPropsPath: File? = null
) {

    val keyStoreFromEnv = System.getenv("STORE_PATH")?.let { File(it) }

    if (keyStoreFromEnv?.exists() == true) {
        println("Using signing data from environment variables.")
        storeFile = keyStoreFromEnv
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS")
        keyPassword = System.getenv("KEY_PASSWORD")
    } else {
        println("Using signing data from properties file.")
        val props = Properties().apply {
            signingPropsPath?.takeIf { it.canRead() }?.let { load(FileInputStream(it)) }
        }

        val keyStorePath = props.getProperty("release.storePath")?.let { File(it) }

        if (keyStorePath?.exists() == true) {
            storeFile = keyStorePath
            storePassword = props.getProperty("release.storePassword")
            keyAlias = props.getProperty("release.keyAlias")
            keyPassword = props.getProperty("release.keyPassword")
        }
    }
}
