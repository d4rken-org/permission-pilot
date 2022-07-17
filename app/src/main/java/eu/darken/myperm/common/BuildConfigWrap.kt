package eu.darken.myperm.common

import eu.darken.myperm.BuildConfig


// Can't be const because that prevents them from being mocked in tests
@Suppress("MayBeConstant")
object BuildConfigWrap {
    val APPLICATION_ID = BuildConfig.APPLICATION_ID
    val DEBUG: Boolean = BuildConfig.DEBUG
    val BUILD_TYPE: BuildType = when (val typ = BuildConfig.BUILD_TYPE) {
        "debug" -> BuildType.DEV
        "beta" -> BuildType.BETA
        "release" -> BuildType.RELEASE
        else -> throw IllegalArgumentException("Unknown buildtype: $typ")
    }

    enum class BuildType {
        DEV,
        BETA,
        RELEASE,
        ;
    }

    val FLAVOR: Flavor = when (val flav = BuildConfig.FLAVOR) {
        "gplay" -> Flavor.GPLAY
        "foss" -> Flavor.FOSS
        else -> throw IllegalStateException("Unknown flavor: $flav")
    }

    enum class Flavor {
        GPLAY,
        FOSS,
        ;
    }

    val VERSION_CODE: Long = BuildConfig.VERSION_CODE.toLong()
    val VERSION_NAME: String = BuildConfig.VERSION_NAME
    val GIT_SHA: String = BuildConfig.GITSHA

    val VERSION_DESCRIPTION: String = "v$VERSION_NAME ($VERSION_CODE) ~ $GIT_SHA/$FLAVOR/$BUILD_TYPE"
}
