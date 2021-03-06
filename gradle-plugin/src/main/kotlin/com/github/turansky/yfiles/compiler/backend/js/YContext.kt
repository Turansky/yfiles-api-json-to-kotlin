package com.github.turansky.yfiles.compiler.backend.js

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.js.backend.ast.JsBlock
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.backend.ast.JsFunction
import org.jetbrains.kotlin.js.backend.ast.JsStatement
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.reference.ReferenceTranslator.translateAsValueReference
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils.getFunctionByName

internal fun TranslationContext.toValueReference(descriptor: DeclarationDescriptor): JsExpression =
    translateAsValueReference(descriptor, this)

internal fun TranslationContext.findFunction(
    packageName: FqName,
    functionName: Name,
): JsExpression {
    val descriptor = getFunctionByName(
        currentModule.getPackage(packageName).memberScope,
        functionName
    )
    return toValueReference(descriptor)
}

internal fun TranslationContext.jsFunction(
    description: String,
    vararg statements: JsStatement,
): JsFunction =
    JsFunction(
        scope(),
        JsBlock(*statements),
        description
    )

internal fun TranslationContext.jsFunction(
    description: String,
    statements: List<JsStatement>,
): JsFunction =
    JsFunction(
        scope(),
        JsBlock(statements),
        description
    )

internal fun TranslationContext.declareConstantValue(
    suggestedName: String,
    value: JsExpression,
): JsExpression =
    declareConstantValue(
        suggestedName,
        suggestedName,
        value,
        null
    )
