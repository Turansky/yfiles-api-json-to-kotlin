package com.github.turansky.yfiles.correction

import com.github.turansky.yfiles.GeneratorContext
import com.github.turansky.yfiles.JS_NUMBER

internal const val EDGE_DIRECTEDNESS = "yfiles.algorithms.EdgeDirectedness"

internal fun generateEdgeDirectednessUtils(context: GeneratorContext) {
    context[EDGE_DIRECTEDNESS] = """
        @JsName("Number")
        external class EdgeDirectedness
        private constructor()
        
        inline fun EdgeDirectedness(value: Double): $EDGE_DIRECTEDNESS = 
            value.unsafeCast<$EDGE_DIRECTEDNESS>()
        
        object EdgeDirectednesses {
            inline val SOURCE_TO_TARGET: $EDGE_DIRECTEDNESS
                get() = EdgeDirectedness(1.0)
            inline val TARGET_TO_SOURCE: $EDGE_DIRECTEDNESS
                get() = EdgeDirectedness(-1.0)
            inline val UNDIRECTED: $EDGE_DIRECTEDNESS
                get() = EdgeDirectedness(0.0)
        }
    """.trimIndent()
}

internal fun applyEdgeDirectednessHacks(source: Source) {
    source.types()
        .optFlatMap(PROPERTIES)
        .filter { it[NAME] == "edgeDirectedness" }
        .forEach { it.replaceInType(",$JS_NUMBER>", ",$EDGE_DIRECTEDNESS>") }

    source.types()
        .optFlatMap(CONSTANTS)
        .filter { it[NAME] == "EDGE_DIRECTEDNESS_DP_KEY" }
        .forEach { it.replaceInType("<$JS_NUMBER>", "<$EDGE_DIRECTEDNESS>") }
}
