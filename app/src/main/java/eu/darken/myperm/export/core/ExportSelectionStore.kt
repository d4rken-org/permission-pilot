package eu.darken.myperm.export.core

import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.debug.logging.logTag
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportSelectionStore @Inject constructor() {

    private val store = ConcurrentHashMap<String, List<String>>()

    fun save(ids: List<String>): String {
        val token = UUID.randomUUID().toString()
        store[token] = ids
        log(TAG) { "Saved ${ids.size} IDs with token=$token" }
        return token
    }

    fun consume(token: String): List<String>? {
        val ids = store.remove(token)
        log(TAG) { "Consumed token=$token, got ${ids?.size ?: 0} IDs" }
        return ids
    }

    companion object {
        private val TAG = logTag("Export", "SelectionStore")
    }
}
