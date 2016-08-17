package com.jetbrains.pluginverifier.utils

import org.objectweb.asm.Type

object MessageUtils {

  private val COMMON_PACKAGES = setOf("java.io", "java.lang", "java.util")

  fun convertClass(className: String): String {
    val binaryName = className.replace('/', '.')
    if (COMMON_PACKAGES.any { binaryName.startsWith(it) }) {
      return binaryName.substringAfterLast('.')
    }
    return binaryName
  }

  /*
    methodName (Ljava/lang/Object;IDF)V -> void methodName(Object, int, double, float)
    className methodName (Ljava/lang/Object;IDF)V -> className.methodName(Object, int, double, float)
  */
  fun convertMethod(methodName: String, methodDescriptor: String, className: String? = null): String {
    val methodType = Type.getMethodType(methodDescriptor)
    val nameAndArgs = methodType.argumentTypes.map { it.className }.joinToString(prefix = "$methodName(", postfix = ")") { convertClass(it) }
    return if (className != null) "$className.$nameAndArgs" else convertClass(methodType.returnType.className) + " " + nameAndArgs
  }

  fun convertField(fieldName: String, fieldDescriptor: String, className: String? = null): String =
      if (className != null)
        "$className.$fieldName"
      else
        convertClass(Type.getType(fieldDescriptor).className) + " " + fieldName

}
