package com.github.materiiapps.enumutil

/**
 * Generate `fromValue(...)` extension methods for the target class.
 * This matches the first enum parameter and returns the matched enum value.
 */
@Target(AnnotationTarget.CLASS)
public annotation class FromValue
