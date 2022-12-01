/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.pluginverifier.results.access.AccessType
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.AnnotationNode

/**
 * Peels an internal JVM descriptor of a type.
 *
 * For arrays returns the innermost type.
 *
 * For primitive types returns `null`.
 *
 * Examples:
 * - `Lcom/example/Example;` -> `com/example/Example`
 * - `[[[Lcom/example/Example;` -> `com/example/Example`
 * - `I`, `D`, `B` -> `null`
 * - `[[I` -> `null`
 * - `V` -> `null`
 */
fun String.extractClassNameFromDescriptor(): String? {
  if (this == "V") {
    return null
  }

  //prepare array name
  val elementType = trimStart('[')

  if (elementType.isPrimitiveType()) {
    return null
  }

  if (elementType.startsWith("L") && elementType.endsWith(";")) {
    return elementType.substring(1, elementType.length - 1)
  }

  return elementType
}

private fun String.isPrimitiveType(): Boolean = length == 1 && first() in "ZIJBFSDC"

fun getAccessType(accessCode: Int): AccessType = when {
  accessCode and Opcodes.ACC_PUBLIC != 0 -> AccessType.PUBLIC
  accessCode and Opcodes.ACC_PRIVATE != 0 -> AccessType.PRIVATE
  accessCode and Opcodes.ACC_PROTECTED != 0 -> AccessType.PROTECTED
  else -> AccessType.PACKAGE_PRIVATE
}

fun List<AnnotationNode>.findAnnotation(className: String): AnnotationNode? =
  find { it.desc?.extractClassNameFromDescriptor() == className }

fun AnnotationNode.getAnnotationValue(key: String): Any? {
  val vls = values ?: return null
  for (i in 0 until vls.size / 2) {
    val k = vls[i * 2]
    val v = vls[i * 2 + 1]
    if (k == key) {
      return v
    }
  }
  return null
}