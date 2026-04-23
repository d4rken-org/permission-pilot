package eu.darken.myperm.permissions.core

import eu.darken.myperm.common.debug.logging.Logging.Priority.WARN
import eu.darken.myperm.common.debug.logging.asLog
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import eu.darken.myperm.permissions.core.container.BasePermission
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

private val TAG = logTag("Permissions", "Repo", "Ext")

/**
 * Permissions as a flat flow.
 *
 * On [PermissionRepo.State.Error] we emit [emptyList] (and log) rather than letting the
 * flow stall — callers would otherwise hang on `.first()` / `combine` indefinitely.
 * Loading is skipped (no emission), preserving the pre-existing "wait for Ready" semantics
 * across refreshes. The full error surface lives on the repo's [PermissionRepo.state]
 * flow for callers that need to distinguish error from empty.
 */
val PermissionRepo.permissions: Flow<Collection<BasePermission>>
    get() = state.mapNotNull { state ->
        when (state) {
            is PermissionRepo.State.Ready -> state.permissions
            is PermissionRepo.State.Error -> {
                log(TAG, WARN) { "permissions flow: upstream in Error, emitting empty. ${state.error.asLog()}" }
                emptyList()
            }
            is PermissionRepo.State.Loading -> null
        }
    }
