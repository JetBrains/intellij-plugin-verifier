package com.jetbrains.plugin.structure.base.utils

import net.bytebuddy.ByteBuddy
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.objectweb.asm.Opcodes.V1_8

internal fun ByteBuddy.emptyClass(fullyQualifiedName: String): ByteArray {
  return subclass(Object::class.java)
    .name(fullyQualifiedName)
    .make()
    .bytes
}

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
