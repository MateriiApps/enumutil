package com.github.materiiapps.enumutil.ksp.example

import com.github.materiiapps.enumutil.FromValue

@FromValue
enum class OpCodes(val code: Int) {
    READY(1),
    DELETE(2),
    CREATE(3),
    DISCONNECT(4);

    companion object Serializer {
        // blah blah blah
    }
}

fun main() {
    val codes = (1..4).toList()
        .map { OpCodes.fromValue(it) }

    println(codes)
}
