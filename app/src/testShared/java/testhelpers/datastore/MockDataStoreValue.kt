package testhelpers.datastore

import eu.darken.myperm.common.datastore.DataStoreValue
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow

inline fun <reified T> mockDataStoreValue(
    defaultValue: T
): DataStoreValue<T> {
    val instance = mockk<DataStoreValue<T>>()
    val flow = MutableStateFlow(defaultValue)
    every { instance.flow } answers { flow }
    coEvery { instance.value() } answers { flow.value }
    coEvery { instance.value(any()) } answers {
        flow.value = arg(0)
    }
    coEvery { instance.update(any()) } answers {
        val transform = arg<(T) -> T>(0)
        flow.value = transform(flow.value)
    }
    every { instance.valueBlocking } answers { flow.value }
    every { instance.valueBlocking = any() } answers {
        flow.value = firstArg()
    }

    return instance
}
