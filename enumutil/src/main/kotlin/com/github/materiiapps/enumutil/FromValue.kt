package com.github.materiiapps.enumutil

/**
 * Generate `fromValue(...)` extension methods for the target class.
 * @param field Match a specific field by name instead of defaulting to the first one.
 */
@Target(AnnotationTarget.CLASS)
public annotation class FromValue(val field: String = "")
