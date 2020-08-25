package com.github.turansky.yfiles.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.starProjectedType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isEnumClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

private val GET_ENUM_NAME = FqName("yfiles.lang.getEnumName")

private val GET_ENUM_VALUES = FqName("yfiles.lang.getEnumValues")
private val GET_ENUM_VALUE_OF = FqName("yfiles.lang.getEnumValueOf")

private val GET_NAME = Name.special("<get-name>")
private val GET_ORDINAL = Name.special("<get-ordinal>")
private val TO_STRING = Name.identifier("toString")

private val VALUES = Name.identifier("values")
private val VALUE_OF = Name.identifier("valueOf")

private val NAMES = setOf(
    GET_NAME,
    GET_ORDINAL,
    TO_STRING,

    VALUES,
    VALUE_OF
)

private val IrClass.isYFilesEnum
    get() = isExternal && isEnumClass
            && superTypes.any { it.getClass()?.isYEnum ?: false }

internal class EnumTransformer(
    private val context: IrPluginContext
) : IrElementTransformerVoid() {
    private val IrFunction.transformRequired: Boolean
        get() {
            val parent = parent as? IrClass
                ?: return false

            return name in NAMES && parent.isYFilesEnum
        }

    override fun visitCall(expression: IrCall): IrExpression {
        val function = expression.symbol.owner
            .takeIf { it.transformRequired }
            ?: return super.visitCall(expression)

        return when (function.name) {
            GET_ORDINAL -> expression.dispatchReceiver!!

            GET_NAME -> createCall(expression, GET_ENUM_NAME)
            TO_STRING -> createCall(expression, GET_ENUM_NAME)

            VALUES -> createStaticCall(expression, GET_ENUM_VALUES)
            VALUE_OF -> createStaticCall(expression, GET_ENUM_VALUE_OF)

            else -> super.visitCall(expression)
        }
    }

    private fun createCall(
        sourceCall: IrCall,
        functionName: FqName
    ): IrCall {
        val parameter = sourceCall.dispatchReceiver!!

        val type = parameter.type
        val enumClass = type.getClass()!!

        val function = context.referenceFunctions(functionName).single()
        val call = IrCallImpl(
            startOffset = sourceCall.startOffset,
            endOffset = sourceCall.endOffset,
            type = type,
            symbol = function
        )

        call.putTypeArgument(0, type)
        call.putValueArgument(0, parameter)
        call.putValueArgument(1, enumClass.companionObjectExpression(sourceCall))

        return call
    }

    private fun createStaticCall(
        sourceCall: IrCall,
        functionName: FqName
    ): IrCall {
        val function = context.referenceFunctions(functionName).single()
        val enumClass = sourceCall.symbol.owner.parent as IrClass

        val hasParameter = sourceCall.valueArgumentsCount == 1
        val resultType = if (hasParameter) {
            enumClass.defaultType
        } else {
            context.symbols.array.starProjectedType
        }

        val call = IrCallImpl(
            startOffset = sourceCall.startOffset,
            endOffset = sourceCall.endOffset,
            type = resultType,
            symbol = function
        )

        call.putTypeArgument(0, enumClass.defaultType)
        call.putValueArgument(0, enumClass.companionObjectExpression(sourceCall))

        if (hasParameter) {
            call.putValueArgument(1, sourceCall.getValueArgument(0))
        }

        return call
    }
}
