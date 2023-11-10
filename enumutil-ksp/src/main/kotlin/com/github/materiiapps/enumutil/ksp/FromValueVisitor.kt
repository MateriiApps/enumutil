package com.github.materiiapps.enumutil.ksp

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toKModifier
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

class FromValueVisitor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : KSVisitorVoid() {
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        if (classDeclaration.classKind != ClassKind.ENUM_CLASS) {
            logger.error("Cannot apply FromValue onto a non enum class!", classDeclaration)
            return
        }

        // Check that target class isn't private
        if (classDeclaration.getVisibility() == Visibility.PRIVATE) {
            logger.error("FromValue: class must not be private", classDeclaration)
            return
        }

        val annotation = classDeclaration.annotations
            .find { it.shortName.asString() == "FromValue" } // I can't find a way to use qualified name, hopefully nobody else uses @FromValue
            ?: throw IllegalStateException("annotation missing; lib broken")

        val targetFieldName = annotation.arguments
            .find { it.name?.getShortName() == "field" }
            ?.value.let { it as String? }
            ?: throw IllegalStateException("annotation missing field; lib broken")

        // If it is the default, use first param
        val targetParam = if (targetFieldName == "") {
            val param = classDeclaration.primaryConstructor
                ?.parameters?.getOrNull(0)

            if (param == null) {
                logger.error("Must have exactly one constructor parameter in order to run FromValue!", classDeclaration)
                return
            }

            param
        } else {
            // Check target param exists
            val param = classDeclaration.primaryConstructor
                ?.parameters
                ?.find { it.name?.getShortName() == targetFieldName }

            if (param == null) {
                logger.error("FromValue: target field does not exist in enum", classDeclaration)
                return
            }

            param
        }

        // Check that the target param is not private
        if (classDeclaration.getDeclaredProperties()
                .find { it.simpleName.asString() == targetFieldName }!!
                .getVisibility() == Visibility.PRIVATE
        ) {
            logger.error("FromValue: target field cannot be private!", classDeclaration)
            return
        }

        // Get all enum entries
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
                Generated FromValue for [%L]
                DO NOT EDIT MANUALLY
                """.trimIndent(),
                classDeclaration.simpleName.asString(),
            )
            .addFunction(
                makeFromValueFunction(
                    companion = companion,
                    parentClass = classDeclaration,
                    param = targetParam,
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
            .addModifiers(parentClass.getVisibility().toKModifier() ?: KModifier.PUBLIC)
            .receiver(companion.toClassName())
            .returns(parentClassName.copy(nullable = true))
            .addParameter("param", param.type.toTypeName())
            .beginControlFlow("return when (param)")
            .apply {
                fields.forEach { field ->
                    val fieldClassName = field.toClassName()

                    addStatement(
                        "%T.%N -> %T",
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
