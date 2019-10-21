package com.github.turansky.yfiles.correction

import com.github.turansky.yfiles.*
import org.json.JSONObject

private fun cursor(generic: String): String =
    "$CURSOR<$generic>"

internal fun applyCursorHacks(source: Source) {
    fixCursor(source)
    fixCursorUtil(source)
}

private fun fixCursor(source: Source) {
    source.type("ICursor")
        .fixGeneric()

    sequenceOf(
        "IEdgeCursor" to EDGE,
        "ILineSegmentCursor" to "yfiles.algorithms.LineSegment",
        "INodeCursor" to NODE,
        "IPointCursor" to YPOINT
    ).forEach { (className, generic) ->
        source.type(className)
            .getJSONArray(J_IMPLEMENTS).apply {
                put(0, getString(0) + "<$generic>")
            }
    }
}

private fun JSONObject.fixGeneric() {
    setSingleTypeParameter("out T", JS_ANY)

    property("current")
        .put(J_TYPE, "T")
}

private fun fixCursorUtil(source: Source) {
    source.type("Cursors").apply {
        jsequence(J_STATIC_METHODS)
            .onEach {
                val name = it.getString(J_NAME)
                val bound = when (name) {
                    "createNodeCursor" -> NODE
                    "createEdgeCursor" -> EDGE
                    else -> JS_ANY
                }

                it.setSingleTypeParameter(bound = bound)
            }
            .forEach {
                it.jsequence(J_PARAMETERS)
                    .plus(it.getJSONObject(J_RETURNS))
                    .filter { it.getString(J_TYPE) == CURSOR }
                    .forEach { it.put(J_TYPE, cursor("T")) }
            }

        staticMethod("toArray").apply {
            sequenceOf(secondParameter, getJSONObject(J_RETURNS))
                .forEach {
                    val type = it.getString(J_TYPE)
                        .replace("<$JS_ANY>", "<T>")

                    it.put(J_TYPE, type)
                }

            secondParameter
                .getJSONArray(J_MODIFIERS)
                .put(OPTIONAL)
        }
    }
}