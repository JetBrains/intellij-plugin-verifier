package com.jetbrains.plugin.structure.classes.resolvers

/**
 * An exception thrown to indicate that a class-file cannot be read
 * using the ASM Java Bytecode engineering library.
 */
class InvalidClassFileException(val className: String, private val asmError: String) : Exception() {

  override val message
    get() = "Unable to read class '$className' using the ASM Java Bytecode engineering library. The internal ASM error: $asmError."
}