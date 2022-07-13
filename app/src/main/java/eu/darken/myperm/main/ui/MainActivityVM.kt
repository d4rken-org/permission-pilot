package eu.darken.myperm.main.ui

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.myperm.common.coroutine.DispatcherProvider
import eu.darken.myperm.common.debug.logging.Logging.Priority.VERBOSE
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.uix.ViewModel2
import eu.darken.myperm.main.core.SomeRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject


@HiltViewModel
class MainActivityVM @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    handle: SavedStateHandle,
    private val someRepo: SomeRepo
) : ViewModel2(dispatcherProvider = dispatcherProvider) {

    private val stateFlow = MutableStateFlow(State())
    val state = stateFlow
        .onEach { log(VERBOSE) { "New state: $it" } }
        .asLiveData2()

    private val readyStateInternal = MutableStateFlow(true)
    val readyState = readyStateInternal.asLiveData2()

    init {
        log { "ViewModel: $ this" }
        log { "SavedStateHandle: ${handle.keys()}" }
        log { "Persisted value: ${handle.get<String>("key")}" }
        handle.set("key", "valueActivity")
    }

    fun onGo() {
        stateFlow.value = stateFlow.value.copy(ready = true)
    }

    data class State(
        val ready: Boolean = false
    )

}