package eu.darken.myperm.common.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Android
import androidx.compose.material.icons.twotone.Shop
import androidx.compose.ui.graphics.vector.ImageVector
import eu.darken.myperm.apps.core.known.AKnownPkg

val AKnownPkg.icon: ImageVector
    get() = when (this) {
        AKnownPkg.GooglePlay -> Icons.TwoTone.Shop
        else -> Icons.TwoTone.Android
    }
