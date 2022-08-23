package eu.darken.myperm.permissions.core

import eu.darken.myperm.permissions.core.container.BasePermission
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map

val PermissionRepo.permissions: Flow<Collection<BasePermission>>
    get() = state.filterIsInstance<PermissionRepo.State.Ready>().map { it.permissions }