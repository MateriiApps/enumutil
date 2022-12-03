package com.github.materiiapps.enumutil.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

class FromValueVisitor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : KSVisitorVoid() {
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        if (classDeclaration.classKind != ClassKind.ENUM_CLASS) {
            logger.error("Cannot run FromValue on a non enum class!", classDeclaration)
            return
        }

        val param = classDeclaration.primaryConstructor?.parameters?.singleOrNull() ?: run {
            logger.error("Must have exactly one constructor parameter in order to run FromValue!", classDeclaration)
            return
        }

        val enumFields = classDeclaration.declarations
            .filter { (it as? KSClassDeclaration)?.classKind == ClassKind.ENUM_ENTRY }
            .map { it as KSClassDeclaration }
            .toList()

        val companion = classDeclaration.declarations
            .find { (it as? KSClassDeclaration)?.isCompanionObject == true }
            .let { it as? KSClassDeclaration }
            ?: run {
                logger.error("Must have companion object in order to run FromValue!", classDeclaration)
                return
            }

        val packageName = classDeclaration.packageName.asString()
        val className = classDeclaration.qualifiedName?.getShortName() ?: "<ERROR>"

        FileSpec.builder(packageName, "${className}FromValue")
            .addFileComment(
                """
                Generated FromValue for [%T]
                DO NOT EDIT MANUALLY
                """.trimIndent(),
                classDeclaration.toClassName()
            )
            .addFunction(
                makeFromValueFunction(
                    companion = companion,
                    parentClass = classDeclaration,
                    param = param,
                    fields = enumFields,
                )
            )
            .clearImports()
            .build()
            .writeTo(
                codeGenerator = codeGenerator,
                dependencies = Dependencies(true, classDeclaration.containingFile!!)
            )
    }

    private fun makeFromValueFunction(
        companion: KSClassDeclaration,
        parentClass: KSClassDeclaration,
        param: KSValueParameter,
        fields: List<KSClassDeclaration>,
    ): FunSpec {
        val parentClassName = parentClass.toClassName()

        return FunSpec.builder("fromValue")
            .receiver(companion.toClassName())
            .returns(parentClassName.copy(nullable = true))
            .addParameter("param", param.type.toTypeName())
            .beginControlFlow("return when (param)")
            .apply {

                fields.forEach { field ->
                    val fieldClassName = field.toClassName()

                    addStatement(
                        "%T.`%L` -> %T",
                        fieldClassName,
                        param.name!!.asString(),
                        fieldClassName,
                    )
                }
            }
            .addStatement("else -> null")
            .endControlFlow()
            .build()
    }
}
