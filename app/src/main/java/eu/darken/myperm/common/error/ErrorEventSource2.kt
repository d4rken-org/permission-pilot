package eu.darken.myperm.common.error

import eu.darken.myperm.common.flow.SingleEventFlow

interface ErrorEventSource2 {
    val errorEvents: SingleEventFlow<Throwable>
}
