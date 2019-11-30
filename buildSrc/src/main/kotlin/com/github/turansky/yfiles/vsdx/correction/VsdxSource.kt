package com.github.turansky.yfiles.vsdx.correction

import com.github.turansky.yfiles.correction.*
import org.json.JSONObject

internal class VsdxSource(private val api: JSONObject) {
    val functionSignatures: JSONObject
        get() = api[J_FUNCTION_SIGNATURES]

    private val types: List<JSONObject> = api
        .jsequence(J_NAMESPACES)
        .jsequence(J_TYPES)
        .toList()

    private val typeMap = types.associateBy { it.uid }

    fun types(): Sequence<JSONObject> =
        types.asSequence()

    fun type(className: String): JSONObject =
        typeMap.getValue(className)

    fun types(vararg classNames: String): Sequence<JSONObject> =
        classNames.asSequence()
            .map { type(it) }

    private val JSONObject.uid: String
        get() = if (has(J_ES6_NAME)) {
            get(J_ES6_NAME)
        } else {
            get(J_NAME)
        }
}
