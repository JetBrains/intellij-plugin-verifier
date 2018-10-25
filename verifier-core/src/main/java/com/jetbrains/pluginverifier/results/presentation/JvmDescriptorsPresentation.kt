package com.jetbrains.pluginverifier.results.presentation

import com.jetbrains.pluginverifier.results.signatures.FormatOptions
import com.jetbrains.pluginverifier.results.signatures.SigVisitor
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
  fun convertMethodSignature(signature: String, binaryNameConverter: String.() -> String): Pair<List<String>, String> {
    require(signature.isNotEmpty()) { "Empty signature is not expected here" }
    val visitor = runSignatureVisitor(signature)
    val methodSignature = visitor.getMethodSignature()
    val formatOptions = FormatOptions(internalNameConverter = binaryNameConverter)
    val returnType = methodSignature.result.format(formatOptions)
    val parameters = methodSignature.parameterSignatures.map { it.format(formatOptions) }
    return parameters to returnType
  }

  private fun runSignatureVisitor(signature: String): SigVisitor {
    require(signature.isNotEmpty())
    val visitor = SigVisitor()
    SignatureReader(signature).accept(visitor)
    return visitor
  }

  fun convertClassSignature(signature: String, binaryNameConverter: String.() -> String): String {
    require(signature.isNotEmpty()) { "Empty signature is not expected here" }
    val visitor = runSignatureVisitor(signature)
    val classSignature = visitor.getClassSignature()
    val formatOptions = FormatOptions(internalNameConverter = binaryNameConverter)
    return classSignature.format(formatOptions)
  }

  fun convertTypeSignature(typeSignature: String, binaryNameConverter: String.() -> String): String {
    require(typeSignature.isNotEmpty()) { "Empty signature is not expected here" }
    val visitor = SigVisitor()
    SignatureReader(typeSignature).acceptType(visitor)
    val fieldSignature = visitor.getFieldSignature()
    val formatOptions = FormatOptions(internalNameConverter = binaryNameConverter)
    return fieldSignature.format(formatOptions)
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
