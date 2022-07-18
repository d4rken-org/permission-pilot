package eu.darken.myperm.apps.core.known

import androidx.annotation.Keep
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.features.AppStore
import eu.darken.myperm.apps.core.features.DisplayablePkg
import kotlin.reflect.full.isSubclassOf

@Keep
sealed class AKnownPkg constructor(override val id: Pkg.Id) : Pkg {
    constructor(rawPkgId: String) : this(Pkg.Id(rawPkgId))

    object AndroidSystem : AKnownPkg("android")

    object GooglePlay : AKnownPkg("com.android.vending"), DisplayablePkg, AppStore {
        override val labelRes: Int = R.string.apps_known_installer_gplay_label
        override val iconRes: Int = R.drawable.ic_baseline_gplay_24
        override val urlGenerator: ((Pkg.Id) -> String) = {
            "https://play.google.com/store/apps/details?id=${it.value}"
        }
    }

    object VivoAppStore : AKnownPkg("com.vivo.appstore"), DisplayablePkg, AppStore {
        override val labelRes: Int = R.string.apps_known_installer_vivo_label
    }

    object OppoMarket : AKnownPkg("com.oppo.market"), DisplayablePkg, AppStore {
        override val labelRes: Int = R.string.apps_known_installer_oppo_label
    }

    object HuaweiAppGallery : AKnownPkg("com.huawei.appmarket"), DisplayablePkg, AppStore {
        override val labelRes: Int = R.string.apps_known_installer_huawei_label
    }

    object SamsungAppStore : AKnownPkg("com.sec.android.app.samsungapps"), DisplayablePkg, AppStore {
        override val labelRes: Int = R.string.apps_known_installer_samsung_label
    }

    object XiaomiAppStore : AKnownPkg("com.xiaomi.mipicks"), DisplayablePkg, AppStore {
        override val labelRes: Int = R.string.apps_known_installer_xiaomi_label
    }

    companion object {
        fun values(): List<AKnownPkg> = AKnownPkg::class.nestedClasses
            .filter { clazz -> clazz.isSubclassOf(AKnownPkg::class) }
            .map { clazz -> clazz.objectInstance }
            .filterIsInstance<AKnownPkg>()
    }
}

fun Pkg.Id.toKnownPkg(): Pkg? = AKnownPkg.values().singleOrNull { it.id == this@toKnownPkg }