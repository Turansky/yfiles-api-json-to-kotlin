package com.github.turansky.yfiles.compiler.extensions

import com.github.turansky.yfiles.compiler.diagnostic.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind.*
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.declaration.DeclarationBodyVisitor
import org.jetbrains.kotlin.js.translate.extensions.JsSyntheticTranslateExtension
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPureClassOrObject
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces

private const val YFILES_TAG = "yfiles"

class YExtension : JsSyntheticTranslateExtension {
    override fun generateClassSyntheticParts(
        declaration: KtPureClassOrObject,
        descriptor: ClassDescriptor,
        translator: DeclarationBodyVisitor,
        context: TranslationContext
    ) {
        if (descriptor.isExternal) {
            return
        }

        when (descriptor.kind) {
            CLASS -> context.generateClass(declaration, descriptor, translator)
            OBJECT, INTERFACE, ENUM_CLASS -> context.checkInterfaces(declaration, descriptor)
            else -> {
                // do nothing
            }
        }
    }
}

private fun TranslationContext.checkInterfaces(
    declaration: KtPureClassOrObject,
    descriptor: ClassDescriptor
) {
    if (descriptor.implementsYFilesInterface) {
        reportError(declaration, BASE_CLASS__INTERFACE_IMPLEMENTING_NOT_SUPPORTED)
    }
}

private fun TranslationContext.generateClass(
    declaration: KtPureClassOrObject,
    descriptor: ClassDescriptor,
    translator: DeclarationBodyVisitor
) {
    when {
        descriptor.extendsYObject ->
            generateCustomYObject(declaration, descriptor)
        descriptor.implementsYFilesInterface ->
            generateBaseClass(declaration, descriptor, translator)
    }
}

private fun TranslationContext.generateCustomYObject(
    declaration: KtPureClassOrObject,
    descriptor: ClassDescriptor
) {
    if (descriptor.getSuperInterfaces().isNotEmpty()) {
        reportError(declaration, YOBJECT__INTERFACE_IMPLEMENTING_NOT_SUPPORTED)
        return
    }

    addDeclarationStatement(fixType(descriptor))
}

private fun TranslationContext.generateBaseClass(
    declaration: KtPureClassOrObject,
    descriptor: ClassDescriptor,
    translator: DeclarationBodyVisitor
) {
    val interfaces = descriptor.getSuperInterfaces()

    when {
        descriptor.isInline ->
            reportError(declaration, BASE_CLASS__INLINE_CLASS_NOT_SUPPORTED)
        descriptor.isData ->
            reportError(declaration, BASE_CLASS__DATA_CLASS_NOT_SUPPORTED)
        descriptor.isCompanionObject ->
            reportError(declaration, BASE_CLASS__COMPANION_OBJECT_NOT_SUPPORTED)

        interfaces.any { !it.isYFiles() } ->
            reportError(declaration, BASE_CLASS__INTERFACE_MIXING_NOT_SUPPORTED)

        declaration.hasExplicitPrimaryConstructor() ->
            reportError(declaration.primaryConstructor!!, BASE_CLASS__CONSTRUCTOR_NOT_SUPPORTED)
        declaration.hasBody() ->
            reportError(declaration, BASE_CLASS__BODY_NOT_SUPPORTED)

        else -> {
            val classId = descriptor.name.identifier
            val baseClassName = generateName(classId, "BaseClass")
            val baseClass = declareConstantValue(
                baseClassName,
                baseClassName,
                baseClass(interfaces, baseClassName),
                null
            )

            translator.addInitializerStatement(baseSuperCall(baseClass))

            declareConstantValue(
                generateName(classId, "prototypeConfigured"),
                generateName(classId, "prototypeConfigured"),
                configurePrototype(descriptor, baseClass),
                null
            ).also { addDeclarationStatement(it.makeStmt()) }
        }
    }
}

private fun KtPureClassOrObject.hasBody(): Boolean =
    declarations.any { it is KtProperty || it is KtNamedFunction }
