package com.github.turansky.yfiles

import org.json.JSONObject

internal fun JSONObject.methodParameters(
    methodName: String,
    parameterName: String,
    parameterFilter: (JSONObject) -> Boolean
): Iterable<JSONObject> {
    val result = getJSONArray("methods")
        .objects { it.getString("name") == methodName }
        .flatMap {
            it.getJSONArray("parameters")
                .objects { it.getString("name") == parameterName }
                .filter(parameterFilter)
        }

    require(result.isNotEmpty())
    { "No method parameters found for object: $this, method: $methodName, parameter: $parameterName" }

    return result
}

internal fun JSONObject.addProperty(
    propertyName: String,
    type: String
) {
    getJSONArray("properties")
        .put(
            mapOf(
                "name" to propertyName,
                "modifiers" to listOf(PUBLIC, FINAL, RO),
                "type" to type
            )
        )
}

internal fun JSONObject.changeNullability(nullable: Boolean) {
    val modifiers = getJSONArray("modifiers")
    val index = modifiers.indexOf(CANBENULL)
    require((index == -1) == nullable)
    if (nullable) {
        modifiers.put(CANBENULL)
    } else {
        modifiers.remove(index)
    }
}

internal fun JSONObject.addStandardGeneric(name: String = "T") {
    put(
        "typeparameters", jArray(
            jObject("name" to name)
        )
    )
}

internal fun JSONObject.jsequence(name: String): Sequence<JSONObject> =
    getJSONArray(name)
        .asSequence()
        .map { it as JSONObject }

internal fun Sequence<JSONObject>.jsequence(name: String): Sequence<JSONObject> =
    flatMap { it.jsequence(name) }

internal fun JSONObject.optionalArray(name: String): Sequence<JSONObject> =
    if (has(name)) {
        jsequence(name)
    } else {
        emptySequence()
    }

internal fun Sequence<JSONObject>.optionalArray(name: String): Sequence<JSONObject> =
    filter { it.has(name) }
        .jsequence(name)

internal val JSONObject.typeParameter: JSONObject
    get() {
        val typeNames = setOf("type", "tType", "itemType")
        return jsequence("parameters")
            .first { it.getString("name") in typeNames }
    }

internal fun JSONObject.parameter(name: String): JSONObject {
    return jsequence("parameters")
        .first { it.getString("name") == name }
}


internal val JSONObject.firstParameter: JSONObject
    get() = getJSONArray("parameters")
        .get(0) as JSONObject

internal fun JSONObject.addGeneric(generic: String) {
    val type = getString("type")
    put("type", "$type<$generic>")
}