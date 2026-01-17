package eu.darken.myperm.apps.core

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.myperm.apps.core.container.*
import eu.darken.myperm.apps.core.known.AKnownPkg
import eu.darken.myperm.common.IPCFunnel
import eu.darken.myperm.common.coroutine.AppScope
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.Logging.Priority.ERROR
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.common.flow.shareLatest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.measureTimedValue

@Singleton
class AppRepo @Inject constructor(
    @ApplicationContext private val context: Context,
    @AppScope private val appScope: CoroutineScope,
    private val dispatcherProvider: DispatcherProvider,
    packageEventListener: PackageEventListener,
    private val ipcFunnel: IPCFunnel,
) {

    private val refreshTrigger = MutableStateFlow(UUID.randomUUID())

    fun refresh() {
        log(TAG) { "refresh() " }
        refreshTrigger.value = UUID.randomUUID()
    }

    val state: Flow<State> = combineTransform<UUID?, PackageEventListener.Event, State>(
        refreshTrigger,
        packageEventListener.events.onStart { emit(PackageEventListener.Event.PackageInstalled(AKnownPkg.AndroidSystem.id)) },
    ) { _, _ ->
        emit(State.Loading())

        val start = System.currentTimeMillis()

        val pkgs = coroutineScope {
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
            listOf(normal.await(), secondaryProfile.await(), uninstalledPkgs.await())
        }

        val allPkgs = pkgs.flatten()
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
                is UninstalledDataPkg -> {
                    curPkg.siblings = siblings
                    curPkg.twins = twins
                }
                else -> throw IllegalArgumentException("Unknown package type: $curPkg")
            }
        }

        val stop = System.currentTimeMillis()
        log(TAG) { "Perf: Total pkgs: ${allPkgs.size} in ${stop - start}ms" }

        emit(State.Ready(pkgs = allPkgs))
    }
        .onStart { emit(State.Loading()) }
        .catch {
            log(TAG, ERROR) { "Failed to generate app data: ${it.asLog()}" }
            throw it
        }
        .shareLatest(scope = appScope, started = SharingStarted.Lazily)

    sealed class State {
        data class Loading(
            val startedAt: Instant = Instant.now()
        ) : State()

        data class Ready(
            val updatedAt: Instant = Instant.now(),
            val pkgs: Collection<BasePkg>,
            val id: UUID = UUID.randomUUID(),
        ) : State()
    }

    companion object {
        internal val TAG = logTag("Apps", "Repo")
    }
}