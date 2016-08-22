package com.jetbrains.pluginverifier.utils

object MessageUtils {

  private val COMMON_PACKAGES = setOf("java.io", "java.lang", "java.util")

  fun convertClass(className: String): String {
    val binaryName = className.replace('/', '.')
    if (COMMON_PACKAGES.any { binaryName.startsWith(it) }) {
      return binaryName.substringAfterLast('.')
    }
    return binaryName
  }

  private fun convertDescriptor(descriptor: String): String {
    var dims = 0
    while (descriptor[dims] == '[') {
      dims++
    }
    val elemType = descriptor.substring(dims)
    val arrayType = when (elemType) {
      "V" -> "void"
      "Z" -> "boolean"
      "C" -> "char"
      "B" -> "byte"
      "S" -> "short"
      "I" -> "int"
      "F" -> "float"
      "J" -> "long"
      "D" -> "double"
      else -> {
        require(elemType.startsWith("L") && elemType.endsWith(";") && elemType.length > 2, { elemType })
        convertClass(elemType.substring(1, elemType.length - 1))
      }
    }
    return arrayType + "[]".repeat(dims)
  }


  private fun convertMethodDescriptor(methodDescriptor: String): Pair<List<String>, String> {
    require(methodDescriptor.startsWith("(") && methodDescriptor.contains(')'), { methodDescriptor })
    val args = arrayListOf<String>()
    var pos = 1
    while (methodDescriptor[pos] != ')') {
      val char = methodDescriptor[pos]
      if (char in "ZCBSIFJD") {
        args.add(convertDescriptor(char.toString()))
        pos++
      } else if (char == '[') {
        var end = pos
        while (methodDescriptor[end] == '[') {
          end++
        }
        if (methodDescriptor[end] == 'L') {
          end = methodDescriptor.indexOf(';', end)
        }
        args.add(convertDescriptor(methodDescriptor.substring(pos, end + 1)))
        pos = end + 1
      } else {
        require(char == 'L')
        val end = methodDescriptor.indexOf(';', pos)
        args.add(convertDescriptor(methodDescriptor.substring(pos, end + 1)))
        pos = end + 1
      }
    }
    val returnType = convertDescriptor(methodDescriptor.substring(pos + 1))
    return args to returnType
  }


  /*
    methodName (Ljava/lang/Object;IDF)V -> void methodName(Object, int, double, float)
    className methodName (Ljava/lang/Object;IDF)V -> className.methodName(Object, int, double, float)
  */
  fun convertMethod(methodName: String, methodDescriptor: String, className: String? = null): String {
    val methodType = convertMethodDescriptor(methodDescriptor)
    val nameAndArgs = methodType.first.joinToString(prefix = "$methodName(", postfix = ")")
    return if (className != null) convertClass(className) + ".$nameAndArgs" else convertClass(methodType.second) + " " + nameAndArgs
  }

  fun convertField(fieldName: String, fieldDescriptor: String, className: String? = null): String =
      if (className != null)
        convertClass(className) + ".$fieldName"
      else
        convertClass(convertDescriptor(fieldDescriptor)) + " " + fieldName

}
