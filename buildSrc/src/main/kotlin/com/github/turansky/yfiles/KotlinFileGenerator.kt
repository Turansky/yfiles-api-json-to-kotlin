package com.github.turansky.yfiles

import java.io.File

internal class KotlinFileGenerator(
    private val types: Iterable<Type>,
    private val functionSignatures: Iterable<FunctionSignature>
) : FileGenerator {
    override fun generate(directory: File) {
        directory.mkdirs()
        directory.deleteRecursively()

        types.forEach {
            val generatedFile = when (it) {
                is Class -> ClassFile(it)
                is Interface -> InterfaceFile(it)
                is Enum -> EnumFile(it)
                else -> throw IllegalStateException("Undefined type for generation: " + it)
            }

            generate(directory, generatedFile)
        }

        functionSignatures
            .groupBy { it.fqn.substringBeforeLast(".") }
            .forEach { _, items -> generate(directory, items) }
    }

    private fun generate(directory: File, generatedFile: GeneratedFile) {
        val data = generatedFile.data
        val dir = directory.resolve(data.path)
        dir.mkdirs()

        val redundantPackageDeclaration = data.packageName + "."

        val file = dir.resolve("${data.jsName}.kt")
        val header = generatedFile.header

        val content = generatedFile.content()
            .replace(redundantPackageDeclaration, "")
        file.writeText("$header\n\n$content")

        val companionContent = generatedFile.companionContent()
            ?: return

        dir.resolve("${data.jsName}Companion.kt")
            .writeText(companionContent.replace(redundantPackageDeclaration, ""))
    }

    private fun generate(
        directory: File,
        signatures: List<FunctionSignature>
    ) {
        val firstData = GeneratorData(signatures.first().fqn)
        val dir = directory.resolve(firstData.path)
        dir.mkdirs()

        val packageName = firstData.packageName
        val redundantPackageDeclaration = packageName + "."

        val file = dir.resolve("Aliases.kt")
        val header = "package $packageName"

        val content = signatures
            .asSequence()
            .sortedBy { it.fqn }
            .map { signature ->
                val typeparameters = signature.typeparameters
                val generics = if (typeparameters.isNotEmpty()) {
                    "<${typeparameters.byComma { it.name }}>"
                } else {
                    ""
                }
                val parameters = signature.parameters
                    .byComma { it.toCode() }
                val returns = signature.returns?.toCode() ?: UNIT

                val data = GeneratorData(signature.fqn)
                "typealias ${data.name}$generics = ($parameters) -> $returns"
                    .replace(redundantPackageDeclaration, "")
            }
            .joinToString("\n\n")

        file.writeText("$header\n\n$content")
    }

    abstract inner class GeneratedFile(private val declaration: Type) {
        val data = es6GeneratorData(declaration)

        protected val typeparameters: List<TypeParameter>
            get() = declaration.typeparameters

        protected val properties: List<Property>
            get() = declaration.properties
                .sortedBy { it.name }

        protected val staticConstants: List<Constant>
            get() = declaration.constants
                .sortedBy { it.name }

        protected val staticProperties: List<Property>
            get() = declaration.staticProperties
                .sortedBy { it.name }

        protected val staticFunctions: List<Method>
            get() = declaration.staticMethods
                .sortedBy { it.name }

        protected val staticDeclarations: List<Declaration>
            get() {
                return sequenceOf<Declaration>()
                    .plus(staticConstants)
                    .plus(staticProperties)
                    .plus(staticFunctions)
                    .toList()
            }

        protected val memberProperties: List<Property>
            get() = properties.filter { !it.static }

        protected val memberFunctions: List<Method>
            get() = declaration.methods
                .sortedBy { it.name }

        protected val memberEvents: List<Event>
            get() = if (declaration is ExtendedType) {
                declaration.events
            } else {
                emptyList()
            }

        protected val memberDeclarations by lazy { calculateMemberDeclarations() }

        protected open fun calculateMemberDeclarations(): List<JsonWrapper> {
            return memberProperties + memberFunctions + memberEvents
        }

        protected val externalAnnotation: String
            get() = "@JsName(\"${data.jsName}\")"

        val header: String
            get() {
                return "@file:JsModule(\"${data.modulePath}\")\n\n" +
                        "package ${data.packageName}\n"
            }

        protected open fun parentTypes(): List<String> {
            return declaration.implementedTypes()
        }

        protected fun parentString(): String {
            val parentTypes = parentTypes()
            if (parentTypes.isEmpty()) {
                return ""
            }
            return ": " + parentTypes.byComma()
        }

        fun genericParameters(): String {
            return declaration.genericParameters()
        }

        open fun isObject() = false

        open fun content(): String {
            return memberDeclarations
                .lines { it.toCode() }
        }

        protected open fun staticContentItems(): List<Declaration> = emptyList()

        protected open fun staticContent(): String {
            if (isObject()) {
                return ""
            }

            val items = staticContentItems()
            val companion = if (items.isNotEmpty()) {
                val code = items.lines {
                    it.toCode()
                }

                """
                |$externalAnnotation
                |external object ${data.name}s {
                |$code
                |}
            """.trimMargin()
            } else {
                ""
            }

            return """
                |$companion
                |
                |$externalAnnotation
                |internal external object ${data.name}Static {
                |    @JsName("\${"$"}class")
                |    val yclass: ${fixPackage("yfiles.lang.Class")}
                |}
            """.trimMargin()
        }

        open fun companionContent(): String? {
            if (isObject()) {
                return null
            }

            val className = data.name
            val yclass = "${className}Static.yclass"

            val result = """
                |package ${data.packageName}
                |
                |val ${constName(className)}_CLASS = $yclass
            """.trimMargin()

            if (data.primitive) {
                return result
            }

            val generics = genericParameters()
            val classDeclaration = className + generics

            return """
                |$result
                |
                |fun Any?.is$className() = ${yclass}.isInstance(this)
                |
                |fun $generics Any?.as$className(): $classDeclaration? =
                |   if (this.is$className()) {
                |       this.unsafeCast<$classDeclaration>()
                |   } else {
                |       null
                |   }
                |
                |fun $generics Any?.to$className(): $classDeclaration =
                |   requireNotNull(this.as$className())
            """.trimMargin()
        }
    }

    inner class ClassFile(private val declaration: Class) : GeneratedFile(declaration) {
        private fun type(): String {
            val modificator = if (memberFunctions.any { it.abstract } || memberProperties.any { it.abstract }) {
                "abstract"
            } else {
                declaration.kotlinModificator
            }

            return modificator + " class"
        }

        // TODO: check after fix
        //  https://youtrack.jetbrains.com/issue/KT-31126
        private fun constructors(): String {
            val constructors = declaration.constructors

            if (constructors.size <= 1) {
                return ""
            }

            return constructors
                .dropLast(1)
                .asSequence()
                .distinct()
                .lines { it.toCode() }
        }

        override fun parentTypes(): List<String> {
            val extendedType = declaration.extendedType()
                ?: return super.parentTypes()

            return sequenceOf(extendedType)
                .plus(super.parentTypes())
                .toList()
        }

        override fun isObject(): Boolean {
            return declaration.constructors.isEmpty() &&
                    memberDeclarations.isEmpty() &&
                    !data.marker
        }

        override fun content(): String {
            if (isObject()) {
                return objectContent()
            }

            if (data.primitive) {
                return staticContent()
            }

            val lastConstructor = declaration.constructors
                .lastOrNull()

            val constructor = if (lastConstructor != null) {
                lastConstructor.toCode()
                    .removePrefix(" constructor")
            } else {
                ""
            }

            val companionObjectContent = if (staticDeclarations.isNotEmpty()) {
                val content = staticDeclarations.lines {
                    it.toCode()
                }

                """
                |companion object {
                |$content
                |}
            """.trimMargin()
            } else {
                ""
            }

            // TODO: add ticket on "UNREACHABLE"
            return "@Suppress(\"UNREACHABLE_CODE\")\n" +
                    "$externalAnnotation\n" +
                    "external ${type()} ${data.name}${genericParameters()} $constructor ${parentString()} {\n" +
                    constructors() + "\n\n" +
                    super.content() + "\n\n" +
                    companionObjectContent + "\n" +
                    "}\n\n\n" +
                    staticContent()
        }

        private fun objectContent(): String {
            val items = staticDeclarations.map {
                it.toCode()
            }

            return """
                |external object ${data.jsName} {
                |    ${items.lines()}
                |}
            """.trimMargin()
        }

        override fun companionContent(): String? {
            val companionContent = super.companionContent()
                ?: return null

            if (data.primitive || data.name == data.jsName) {
                return companionContent
            }

            val generics = genericParameters()
            return companionContent + "\n\n" +
                    "typealias ${data.jsName}$generics = ${data.name}$generics"
        }
    }

    inner class InterfaceFile(declaration: Interface) : GeneratedFile(declaration) {
        override fun calculateMemberDeclarations(): List<JsonWrapper> {
            return memberProperties.filter { it.abstract } +
                    memberFunctions.filter { it.abstract } +
                    memberEvents
        }

        override fun content(): String {
            val content = super.content()
                .replace("abstract ", "")

            return "$externalAnnotation\n" +
                    "external interface ${data.name}${genericParameters()}${parentString()} {\n" +
                    content + "\n" +
                    "}\n\n" +
                    staticContent()
        }

        override fun staticContentItems(): List<Declaration> = staticDeclarations

        override fun staticContent(): String {
            val staticContent = super.staticContent()

            val generics = genericParameters()
            return """
                |$staticContent
                |
                |$externalAnnotation
                |internal external class ${data.name}Delegate$generics(source: ${data.name}$generics)
                |
            """.trimMargin()
        }

        private val defaultDeclarations = memberProperties.filter { !it.abstract } +
                memberFunctions.filter { !it.abstract }

        override fun companionContent(): String? {
            var content = requireNotNull(super.companionContent())

            val generics = genericParameters()
            val classDeclaration = data.name + generics
            val delegateClassDeclaration = data.name + "Delegate" + generics

            content += """
                |
                |fun $generics yy(source:$classDeclaration) : $classDeclaration {
                |   return $delegateClassDeclaration(source).unsafeCast<$classDeclaration>()
                |}
                |
                |fun $generics $classDeclaration.yCast() = yy(this)
            """.trimMargin()

            if (defaultDeclarations.isEmpty()) {
                return content
            }

            val extensions = defaultDeclarations
                .lines {
                    when (it) {
                        is Property -> it.toExtensionCode(classDeclaration, typeparameters)
                        is Method -> it.toExtensionCode(classDeclaration, typeparameters)
                        else -> throw IllegalStateException("Invalid default declaration")
                    }
                }

            return "$content\n\n$extensions"
        }
    }

    inner class EnumFile(private val declaration: Enum) : GeneratedFile(declaration) {
        override fun content(): String {
            val values = declaration.constants
                .asSequence()
                .map { "    ${it.name}" }
                .joinToString(separator = ",\n", postfix = ";\n")

            return "$externalAnnotation\n" +
                    "external enum class ${data.name} {\n" +
                    values + "\n" +
                    super.content() + "\n" +
                    "}"
        }

        override fun isObject() = true
    }
}