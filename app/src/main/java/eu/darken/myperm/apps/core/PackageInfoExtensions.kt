package eu.darken.myperm.apps.core

import android.content.pm.ApplicationInfo

val ApplicationInfo.isSystemApp: Boolean
    get() = this.flags and ApplicationInfo.FLAG_SYSTEM != 0