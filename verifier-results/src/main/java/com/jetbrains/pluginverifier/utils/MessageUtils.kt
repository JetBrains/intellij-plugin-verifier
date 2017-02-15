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

  fun parseMethodParameters(methodDescriptor: String): List<String> {
    require(methodDescriptor.startsWith("(") && methodDescriptor.contains(')'), { methodDescriptor })
    val params = arrayListOf<String>()
    var pos = 1
    while (methodDescriptor[pos] != ')') {
      val char = methodDescriptor[pos]
      if (char in "ZCBSIFJD") {
        params.add(char.toString())
        pos++
      } else if (char == '[') {
        var end = pos
        while (methodDescriptor[end] == '[') {
          end++
        }
        if (methodDescriptor[end] == 'L') {
          end = methodDescriptor.indexOf(';', end)
        }
        params.add(methodDescriptor.substring(pos, end + 1))
        pos = end + 1
      } else {
        require(char == 'L')
        val end = methodDescriptor.indexOf(';', pos)
        params.add(methodDescriptor.substring(pos, end + 1))
        pos = end + 1
      }
    }
    return params
  }

  private fun convertMethodDescriptor(methodDescriptor: String): Pair<List<String>, String> {
    val parameters = parseMethodParameters(methodDescriptor).map { convertDescriptor(it) }
    val returnType = convertDescriptor(methodDescriptor.substringAfter(")"))
    return parameters to returnType
  }

  /*
    methodName (Ljava/lang/Object;IDF)V -> void methodName(Object, int, double, float)
    className methodName (Ljava/lang/Object;IDF)V -> className.methodName(Object, int, double, float)
  */
  fun convertMethod(methodName: String, methodDescriptor: String, className: String, parameterNames: List<String>? = null): String {
    val (parameters, returnType) = convertMethodDescriptor(methodDescriptor)
    require(parameterNames == null || parameterNames.size == parameters.size, { "Missing parameter names: $methodDescriptor : $parameterNames" })
    val names = parameterNames ?: (0..parameters.size - 1).map { "arg$it" }
    val parametersWithNames = parameters.zip(names).map { "${it.first} ${it.second}" }
    return convertClass(className) + ".$methodName(" + parametersWithNames.joinToString() + ") : $returnType"
  }

  fun convertField(fieldName: String, className: String): String = convertClass(className) + ".$fieldName"

}
