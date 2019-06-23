package com.github.turansky.yfiles

fun between(str: String, start: String, end: String, firstEnd: Boolean = false): String {
    val startIndex = str.indexOf(start)
    require(startIndex != -1)
    { "String '$str' doesn't contains '$start'" }

    val endIndex = if (firstEnd) {
        str.indexOf(end)
    } else {
        str.lastIndexOf(end)
    }
    require(endIndex != -1)
    { "String '$str' doesn't contains '$end'" }

    return str.substring(startIndex + start.length, endIndex)
}

fun till(str: String, end: String): String {
    val endIndex = str.indexOf(end)
    require(endIndex != -1)
    { "String '$str' doesn't contains '$end'" }

    return str.substring(0, endIndex)
}

fun constName(str: String): String {
    return str
        .replace(Regex("([a-z])([A-Z])"), "\$1_\$2")
        .toUpperCase()
}

@Suppress("NOTHING_TO_INLINE")
inline fun exp(condition: Boolean, str: String): String {
    return if (condition) str else ""
}