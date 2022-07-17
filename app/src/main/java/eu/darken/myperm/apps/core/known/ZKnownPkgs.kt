package eu.darken.myperm.apps.core.known

import androidx.annotation.Keep
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.types.AppStore
import eu.darken.myperm.apps.core.types.DisplayablePkg
import kotlin.reflect.full.isSubclassOf

@Keep
sealed class ZKnownPkgs constructor(override val id: Pkg.Id) : Pkg {
    constructor(rawPkgId: String) : this(Pkg.Id(rawPkgId))

    object GooglePlay : ZKnownPkgs("com.android.vending"), DisplayablePkg, AppStore {
        override val labelRes: Int = R.string.apps_known_installer_gplay_label
        override val iconRes: Int = R.drawable.ic_baseline_gplay_24
        override val urlGenerator: ((Pkg.Id) -> String) = {
            "https://play.google.com/store/apps/details?id=${it.value}"
        }
    }

    object VivoAppStore : ZKnownPkgs("com.vivo.appstore"), DisplayablePkg, AppStore {
        override val labelRes: Int = R.string.apps_known_installer_vivo_label
    }

    object OppoMarket : ZKnownPkgs("com.oppo.market"), DisplayablePkg, AppStore {
        override val labelRes: Int = R.string.apps_known_installer_oppo_label
    }

    object HuaweiAppGallery : ZKnownPkgs("com.huawei.appmarket"), DisplayablePkg, AppStore {
        override val labelRes: Int = R.string.apps_known_installer_huawei_label
    }

    object SamsungAppStore : ZKnownPkgs("com.sec.android.app.samsungapps"), DisplayablePkg, AppStore {
        override val labelRes: Int = R.string.apps_known_installer_samsung_label
    }

    object XiaomiAppStore : ZKnownPkgs("com.xiaomi.mipicks"), DisplayablePkg, AppStore {
        override val labelRes: Int = R.string.apps_known_installer_xiaomi_label
    }

    companion object {
        fun values(): List<ZKnownPkgs> = ZKnownPkgs::class.nestedClasses
            .filter { clazz -> clazz.isSubclassOf(ZKnownPkgs::class) }
            .map { clazz -> clazz.objectInstance }
            .filterIsInstance<ZKnownPkgs>()
    }
}

fun Pkg.Id.toKnownPkg(): Pkg? = ZKnownPkgs.values().singleOrNull { it.id == this@toKnownPkg }