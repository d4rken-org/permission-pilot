package eu.darken.myperm.common.error

import eu.darken.myperm.common.livedata.SingleLiveEvent

interface ErrorEventSource {
    val errorEvents: SingleLiveEvent<Throwable>
}