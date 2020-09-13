package com.github.turansky.yfiles.ide.binding

import com.github.turansky.yfiles.ide.psi.DefaultPsiFinder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.xml.XmlAttributeValue

internal class ContextClassReference(
    element: XmlAttributeValue,
    rangeInElement: TextRange,
    private val className: String
) : PsiReferenceBase<XmlAttributeValue>(element, rangeInElement, true) {
    override fun resolve(): PsiElement? =
        DefaultPsiFinder.findClass(element, className)
}

internal class ContextPropertyReference(
    element: XmlAttributeValue,
    rangeInElement: TextRange,
    private val property: IProperty
) : PsiReferenceBase<XmlAttributeValue>(element, rangeInElement, property.isStandard) {
    override fun resolve(): PsiElement? =
        if (property.isStandard) {
            DefaultPsiFinder.findProperty(element, property.className, property.name)
        } else {
            null
        }

    override fun getVariants(): Array<out Any> =
        DefaultPsiFinder.findPropertyVariants(element, CONTEXT_CLASSES)
            ?: CONTEXT_PROPERTY_VARIANTS
}
