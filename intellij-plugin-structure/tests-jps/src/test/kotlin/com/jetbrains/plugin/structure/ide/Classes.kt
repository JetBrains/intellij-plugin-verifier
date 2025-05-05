/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.objectweb.asm.Opcodes.V1_8

internal fun createEmptyClass(fullyQualifiedBinaryName: String): ByteArray {
  return ClassWriter(0).apply {
    visit(
      V1_8,
      ACC_PUBLIC, // Class access modifier
      fullyQualifiedBinaryName, // Full binary name (package/class)
      null, // Signature (optional, used for generics - null for now)
      "java/lang/Object", // Superclass (default is java.lang.Object)
      null // Interfaces (null as there are none)
    )
    visitEnd()
  }.toByteArray()
}
