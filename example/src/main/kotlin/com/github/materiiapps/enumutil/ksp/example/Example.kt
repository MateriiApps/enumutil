package com.github.materiiapps.enumutil.ksp.example

import com.github.materiiapps.enumutil.FromValue

@FromValue("code")
enum class OpCodes(val code: Int, val display: String) {
    READY(1, "Ready"),
    DELETE(2, "Delete"),
    CREATE(3, "Create"),
    DISCONNECT(4, "Disconnect");

    companion object Serializer {
        // blah blah blah
    }
}

fun main() {
    val codes = (1..4).toList()
        .map { OpCodes.fromValue(it) }

    println(codes)
}
