package com.jetbrains.pluginverifier.results.presentation

import org.objectweb.asm.signature.SignatureReader

object JvmDescriptorsPresentation {

  /**
   * Converts internal JVM type-descriptor into Java-like type.
   * E.g.
   * I -> int
   * D -> double
   * ...
   * '[[[Ljava/lang/Object;' -> 'java.lang.Object[][][]'
   */
  fun convertJvmDescriptorToNormalPresentation(descriptor: String, binaryNameConverter: String.() -> String): String {
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
        require(elemType.startsWith("L") && elemType.endsWith(";") && elemType.length > 2) { elemType }
        elemType.substring(1, elemType.length - 1).binaryNameConverter()
      }
    }
    return arrayType + "[]".repeat(dims)
  }

  /**
   * Splits internal JVM descriptor on individual descriptors of the parameters and return type.
   *
   * E.g. (IFLjava/lang/Object;)Ljava/lang/String; -> [I, F, Ljava/lang/Object;] and Ljava/lang/String;
   */
  fun splitMethodDescriptorOnRawParametersAndReturnTypes(methodDescriptor: String): Pair<List<String>, String> {
    val parameterTypes = parseMethodParametersTypesByDescriptor(methodDescriptor)
    val returnType = methodDescriptor.substringAfter(")")
    return parameterTypes to returnType
  }

  /**
   * Splits generics signature of the method on method's parameters and return type
   *
   * E.g. (I)TE; -> [ int ] and E
   */
  fun parseMethodSignature(methodSignature: String, binaryNameConverter: String.() -> String): Pair<List<String>, String> {
    require(methodSignature.isNotEmpty()) { "Empty signature is not expected here" }
    val visitor = runSignatureVisitor(methodSignature, binaryNameConverter)
    val returnType = visitor.getReturnType()
    return visitor.getMethodParameterTypes() to returnType
  }

  private fun runSignatureVisitor(signature: String, binaryNameConverter: String.() -> String): PresentableSignatureVisitor {
    require(signature.isNotEmpty())
    val visitor = PresentableSignatureVisitor(binaryNameConverter)
    SignatureReader(signature).accept(visitor)
    return visitor
  }

  fun parseClassSignature(classSignature: String, binaryNameConverter: String.() -> String): String {
    require(classSignature.isNotEmpty()) { "Empty signature is not expected here" }
    val visitor = runSignatureVisitor(classSignature, binaryNameConverter)
    return visitor.getClassFormalTypeParameters()
  }

  fun convertTypeSignature(typeSignature: String, binaryNameConverter: String.() -> String): String {
    require(typeSignature.isNotEmpty()) { "Empty signature is not expected here" }
    val visitor = TypeSignatureVisitor(binaryNameConverter)
    SignatureReader(typeSignature).acceptType(visitor)
    return visitor.getResult()
  }

  private fun parseMethodParametersTypesByDescriptor(methodDescriptor: String): List<String> {
    require(methodDescriptor.startsWith("(") && methodDescriptor.contains(')')) { "Invalid method descriptor: $methodDescriptor" }
    val rawParameterTypes = arrayListOf<String>()
    var pos = 1
    while (methodDescriptor[pos] != ')') {
      val char = methodDescriptor[pos]
      when (char) {
        in "ZCBSIFJD" -> {
          rawParameterTypes.add(char.toString())
          pos++
        }
        '[' -> {
          var end = pos
          while (methodDescriptor[end] == '[') {
            end++
          }
          if (methodDescriptor[end] == 'L') {
            end = methodDescriptor.indexOf(';', end)
          }
          rawParameterTypes.add(methodDescriptor.substring(pos, end + 1))
          pos = end + 1
        }
        else -> {
          require(char == 'L')
          val end = methodDescriptor.indexOf(';', pos)
          rawParameterTypes.add(methodDescriptor.substring(pos, end + 1))
          pos = end + 1
        }
      }
    }
    return rawParameterTypes
  }

}
