package eu.darken.myperm.common

import android.content.Context

interface HasDescription {
    fun getDescription(context: Context): String
}