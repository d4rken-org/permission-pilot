plugins {
    id("com.android.application") version "9.0.1"
}

val versionCodeProp = (project.findProperty("versionCode") as? String)?.toIntOrNull() ?: 1

android {
    namespace = "eu.darken.myperm.testapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "eu.darken.myperm.testapp"
        minSdk = 23
        targetSdk = 36
        versionCode = versionCodeProp
        versionName = "1.0.$versionCodeProp"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
