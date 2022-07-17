package eu.darken.myperm.apps.core.types

import android.content.Context

interface HasLabel {
    fun getLabel(context: Context): String
}