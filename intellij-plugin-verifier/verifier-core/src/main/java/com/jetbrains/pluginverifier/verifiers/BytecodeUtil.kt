package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.pluginverifier.results.access.AccessType
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.MethodNode

fun MethodNode.getParameterNames(): List<String> {
  val arguments = Type.getArgumentTypes(desc)
  val argumentsNumber = arguments.size
  val offset = if (this.access and Opcodes.ACC_STATIC != 0) 0 else 1
  var parameterNames: List<String> = emptyList()
  if (localVariables != null) {
    parameterNames = localVariables.map { it.name }.drop(offset).take(argumentsNumber)
  }
  if (parameterNames.size != argumentsNumber) {
    parameterNames = (0 until argumentsNumber).map { "arg$it" }
  }
  return parameterNames
}

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