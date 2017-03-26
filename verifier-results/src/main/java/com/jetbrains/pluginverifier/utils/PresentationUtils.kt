package com.jetbrains.pluginverifier.utils

import org.jetbrains.intellij.plugins.internal.asm.signature.SignatureReader

object PresentationUtils {

  /**
   * Converts class name in binary form into Java-like presentation.
   * E.g. 'org/some/Class$Inner1$Inner2' -> 'org.some.Class.Inner1.Inner2'
   */
  val normalConverter: (String) -> String = { binaryName -> binaryName.replace('/', '.').replace('$', '.') }

  /**
   * Cuts off the package of the class and converts the simple name of the class to Java-like presentation
   * E.g. 'org/some/Class$Inner1$Inner2' -> 'Class.Inner1.Inner2'
   */
  val cutPackageConverter: (String) -> String = { binaryName -> binaryName.substringAfterLast("/").replace('$', '.') }

  /**
   * Converts internal JVM type-descriptor into Java-like type.
   * E.g.
   * I -> int
   * D -> double
   * ...
   * '[[[Ljava/lang/Object;' -> 'java.lang.Object[][][]'
   */
  fun convertJvmDescriptorToNormalPresentation(descriptor: String, binaryNameConverter: (String) -> String): String {
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
        binaryNameConverter(elemType.substring(1, elemType.length - 1))
      }
    }
    return arrayType + "[]".repeat(dims)
  }

  /**
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
  fun parseMethodSignature(methodSignature: String, binaryNameConverter: (String) -> String): Pair<List<String>, String> {
    require(methodSignature.isNotEmpty(), { "Empty signature is not expected here" })
    val visitor = runSignatureVisitor(methodSignature, binaryNameConverter)
    val returnType = visitor.getReturnType()
    return visitor.getMethodParameterTypes() to returnType
  }

  private fun runSignatureVisitor(signature: String, binaryNameConverter: (String) -> String): PresentableSignatureVisitor {
    require(signature.isNotEmpty())
    val visitor = PresentableSignatureVisitor(binaryNameConverter)
    SignatureReader(signature).accept(visitor)
    return visitor
  }

  fun convertClassSignature(classSignature: String, binaryNameConverter: (String) -> String): String {
    require(classSignature.isNotEmpty(), { "Empty signature is not expected here" })
    val visitor = runSignatureVisitor(classSignature, binaryNameConverter)
    return visitor.getClassFormalTypeParameters()
  }

  fun convertFieldSignature(fieldSignature: String, binaryNameConverter: (String) -> String): String {
    require(fieldSignature.isNotEmpty(), { "Empty signature is not expected here" })
    val visitor = TypeSignatureVisitor(binaryNameConverter)
    SignatureReader(fieldSignature).acceptType(visitor)
    return visitor.getResult()
  }

  private fun parseMethodParametersTypesByDescriptor(methodDescriptor: String): List<String> {
    require(methodDescriptor.startsWith("(") && methodDescriptor.contains(')'), { "Invalid method descriptor: $methodDescriptor" })
    val rawParameterTypes = arrayListOf<String>()
    var pos = 1
    while (methodDescriptor[pos] != ')') {
      val char = methodDescriptor[pos]
      if (char in "ZCBSIFJD") {
        rawParameterTypes.add(char.toString())
        pos++
      } else if (char == '[') {
        var end = pos
        while (methodDescriptor[end] == '[') {
          end++
        }
        if (methodDescriptor[end] == 'L') {
          end = methodDescriptor.indexOf(';', end)
        }
        rawParameterTypes.add(methodDescriptor.substring(pos, end + 1))
        pos = end + 1
      } else {
        require(char == 'L')
        val end = methodDescriptor.indexOf(';', pos)
        rawParameterTypes.add(methodDescriptor.substring(pos, end + 1))
        pos = end + 1
      }
    }
    return rawParameterTypes
  }

}
