package com.github.materiiapps.enumutil.ksp

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate

internal class ProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return Processor(environment)
    }

    class Processor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
        override fun process(resolver: Resolver): List<KSAnnotated> {
            val visitors = mapOf(
                FROMVALUE_ANNOTATION to ::FromValueVisitor
            )

            val unprocessable = mutableListOf<KSAnnotated>()

            for ((annotation, visitor) in visitors) {
                val (success, fail) = resolver.getSymbolsWithAnnotation(annotation)
                    .partition { it.validate() && it is KSClassDeclaration }

                unprocessable += fail
                success.forEach {
                    it.accept(visitor.invoke(environment.codeGenerator, environment.logger), Unit)
                }
            }

            return unprocessable
        }

        companion object {
            const val FROMVALUE_ANNOTATION = "com.github.materiiapps.enumutil.ksp.FromValue"
        }
    }
}
