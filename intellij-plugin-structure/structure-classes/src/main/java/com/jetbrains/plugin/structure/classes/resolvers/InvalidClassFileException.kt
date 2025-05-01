/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.classes.resolvers

/**
 * An exception thrown to indicate that a class-file cannot be read
 * using the ASM Java Bytecode engineering library.
 */
class InvalidClassFileException(val className: CharSequence, private val asmError: String) : Exception() {

  override val message
    get() = "Unable to read class '$className' using the ASM Java Bytecode engineering library. The internal ASM error: $asmError."
}