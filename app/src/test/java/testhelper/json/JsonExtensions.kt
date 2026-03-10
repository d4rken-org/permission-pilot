package testhelper.json

import kotlinx.serialization.json.Json

private val prettyJson = Json { prettyPrint = true }

fun String.toComparableJson(): String {
    val element = Json.parseToJsonElement(this)
    return prettyJson.encodeToString(element)
}
