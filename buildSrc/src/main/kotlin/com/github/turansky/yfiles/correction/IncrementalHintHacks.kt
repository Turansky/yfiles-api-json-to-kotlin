package com.github.turansky.yfiles.correction

import com.github.turansky.yfiles.JS_ANY
import com.github.turansky.yfiles.json.get
import java.io.File

internal val INCREMENTAL_HINT = "yfiles.hierarchic.IncrementalHint"

internal fun generateIncrementalHint(sourceDir: File) {
    sourceDir.resolve("yfiles/hierarchic/IncrementalHint.kt")
        .writeText(
            // language=kotlin
            """
                |package yfiles.hierarchic
                |
                |external interface IncrementalHint
            """.trimMargin()
        )
}

internal fun applyIncrementalHintHacks(source: Source) {
    source.type("IIncrementalHintsFactory")
        .flatMap(METHODS)
        .forEach { it[RETURNS][TYPE] = INCREMENTAL_HINT }

    source.type("IncrementalHintItemMapping").also {
        it[EXTENDS] = it[EXTENDS].replace(",$JS_ANY,", ",$INCREMENTAL_HINT,")

        it[METHODS]["provideMapperForContext"]
            .get(RETURNS)
            .also { it[TYPE] = it[TYPE].replace(",$JS_ANY>", ",$INCREMENTAL_HINT>") }
    }

    source.types("HierarchicLayout", "HierarchicLayoutCore")
        .map { it[CONSTANTS]["INCREMENTAL_HINTS_DP_KEY"] }
        .forEach { it[TYPE] = it[TYPE].replace("<$JS_ANY>", "<$INCREMENTAL_HINT>") }

    source.type("INodeData")[PROPERTIES]["incrementalHint"][TYPE] = INCREMENTAL_HINT
}