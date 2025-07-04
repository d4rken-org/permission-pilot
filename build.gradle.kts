buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.11.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.Kotlin.core}")
        classpath("com.google.dagger:hilt-android-gradle-plugin:${Versions.Dagger.core}")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:${Versions.AndroidX.Navigation.core}")
    }
}

plugins {
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

tasks.register("clean").configure {
    delete("build")
}