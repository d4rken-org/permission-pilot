package eu.darken.myperm.apps.core

import eu.darken.myperm.apps.core.container.BasePkg
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map


val AppRepo.apps: Flow<Collection<BasePkg>>
    get() = state.filterIsInstance<AppRepo.State.Ready>().map { it.pkgs }