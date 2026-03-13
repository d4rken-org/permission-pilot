package eu.darken.myperm.apps.core

import eu.darken.myperm.apps.core.container.BasePkg
import eu.darken.myperm.apps.core.container.PrimaryProfilePkg
import eu.darken.myperm.apps.core.container.SecondaryProfilePkg
import eu.darken.myperm.apps.core.container.SecondaryUserPkg
import eu.darken.myperm.apps.core.container.UninstalledDataPkg
import eu.darken.myperm.apps.core.container.getNormalPkgs
import eu.darken.myperm.apps.core.container.getSecondaryProfilePkgs
import eu.darken.myperm.apps.core.container.getSecondaryUserPkgs
import eu.darken.myperm.common.IPCFunnel
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.measureTimedValue

@Singleton
class AppSourcer @Inject constructor(
    private val ipcFunnel: IPCFunnel,
    private val dispatcherProvider: DispatcherProvider,
) {

    suspend fun scanPackages(): List<BasePkg> = coroutineScope {
        val normal = async(dispatcherProvider.Default) {
            measureTimedValue {
                getNormalPkgs(ipcFunnel)
            }.let {
                log(TAG) { "Perf: Primary profile pkgs took ${it.duration.inWholeMilliseconds}ms" }
                it.value
            }
        }
        val secondaryProfile = async(dispatcherProvider.Default) {
            measureTimedValue {
                getSecondaryProfilePkgs(ipcFunnel)
            }.let {
                log(TAG) { "Perf: Secondary profile pkgs took ${it.duration.inWholeMilliseconds}ms" }
                it.value
            }
        }
        val uninstalledPkgs = async(dispatcherProvider.Default) {
            measureTimedValue {
                getSecondaryUserPkgs(ipcFunnel).filter { uninstalled ->
                    secondaryProfile.await().none { it.id.pkgName == uninstalled.id.pkgName }
                }
            }.let {
                log(TAG) { "Perf: Secondary user pkgs took ${it.duration.inWholeMilliseconds}ms" }
                it.value
            }
        }
        awaitAll(normal, secondaryProfile, uninstalledPkgs)
        val allPkgs = listOf(normal.await(), secondaryProfile.await(), uninstalledPkgs.await()).flatten()

        allPkgs.forEach { curPkg ->
            val twins = allPkgs.asSequence()
                .filter { it != curPkg }
                .filter { curPkg.id.pkgName == it.id.pkgName && curPkg.id.userHandle != it.id.userHandle }
                .toSet()

            val siblings = allPkgs.asSequence()
                .filter { it != curPkg }
                .filter {
                    if (it.sharedUserId == null) return@filter false
                    it.sharedUserId == curPkg.sharedUserId && it.id.userHandle == curPkg.id.userHandle
                }
                .toSet()

            when (curPkg) {
                is PrimaryProfilePkg -> { curPkg.siblings = siblings; curPkg.twins = twins }
                is SecondaryProfilePkg -> { curPkg.siblings = siblings; curPkg.twins = twins }
                is SecondaryUserPkg -> { curPkg.siblings = siblings; curPkg.twins = twins }
                is UninstalledDataPkg -> { curPkg.siblings = siblings; curPkg.twins = twins }
            }
        }

        allPkgs
    }

    companion object {
        private val TAG = logTag("Apps", "Sourcer")
    }
}
