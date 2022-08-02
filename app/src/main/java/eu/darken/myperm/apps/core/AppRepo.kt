package eu.darken.myperm.apps.core

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.core.container.*
import eu.darken.myperm.apps.core.known.AKnownPkg
import eu.darken.myperm.common.coroutine.AppScope
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.flow.shareLatest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepo @Inject constructor(
    @ApplicationContext private val context: Context,
    @AppScope private val appScope: CoroutineScope,
    packageEventListener: PackageEventListener,
) {

    private val refreshTrigger = MutableStateFlow(UUID.randomUUID())

    fun refresh() {
        log(TAG) { "refresh() " }
        refreshTrigger.value = UUID.randomUUID()
    }

    val apps: Flow<Collection<BasePkg>> = combine(
        refreshTrigger,
        packageEventListener.events.onStart { emit(PackageEventListener.Event.PackageInstalled(AKnownPkg.AndroidSystem.id)) },
    ) { _, _ ->
        val normalPkgs = context.getNormalPkgs()

        val profilePkgs = context.getSecondaryProfilePkgs()

        val uninstalledPkgs = context.getSecondaryUserPkgs().filter { uninstalled ->
            profilePkgs.none { it.id.pkgName == uninstalled.id.pkgName }
        }

        val allPkgs = normalPkgs + profilePkgs + uninstalledPkgs
        allPkgs.forEach { curPkg ->

            val twins = allPkgs.asSequence()
                .filter { it != curPkg } // Don't compare against ourselves
                .filter {
                    curPkg.id.pkgName == it.id.pkgName && curPkg.id.userHandle != it.id.userHandle
                }
                .toSet()

            val siblings = allPkgs.asSequence()
                .filter { it != curPkg } // Don't compare against ourselves
                .filter {
                    if (it.sharedUserId == null) return@filter false
                    if (it.sharedUserId == null) return@filter false

                    it.sharedUserId == curPkg.sharedUserId && it.id.userHandle == curPkg.id.userHandle
                }
                .toSet()

            when (curPkg) {
                is PrimaryProfilePkg -> {
                    curPkg.siblings = siblings
                    curPkg.twins = twins
                }
                is SecondaryProfilePkg -> {
                    curPkg.siblings = siblings
                    curPkg.twins = twins
                }
                is SecondaryUserPkg -> {
                    curPkg.siblings = siblings
                    curPkg.twins = twins
                }
                else -> throw IllegalArgumentException("Unknown package type: $curPkg")
            }
        }

        log { "Total pkgs: ${allPkgs.size}" }

        allPkgs
    }.shareLatest(scope = appScope, started = SharingStarted.Lazily)


    companion object {
        internal val TAG = logTag("Apps", "Repo")
    }
}