package eu.darken.myperm.common

import android.content.Context

interface HasLabel {
    fun getLabel(context: Context): String
}