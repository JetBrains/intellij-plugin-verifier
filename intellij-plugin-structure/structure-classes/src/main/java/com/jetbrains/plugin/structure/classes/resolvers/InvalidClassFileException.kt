package com.jetbrains.plugin.structure.classes.resolvers

import java.io.IOException

/**
 * An exception thrown to indicate that a class-file cannot be read
 * using the ASM Java Bytecode engineering library.
 */
class InvalidClassFileException(val className: String, private val asmError: String) : IOException() {

  override val message
    get() = "Unable to read class-file `$className` using the ASM Java Bytecode engineering library. The internal ASM error: $asmError."
}