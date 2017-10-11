package com.jetbrains.pluginverifier.results.presentation

import org.objectweb.asm.Opcodes
import org.objectweb.asm.signature.SignatureVisitor

class TypeSignatureVisitor(val binaryNameConverter: (String) -> String) : SignatureVisitor(Opcodes.ASM5) {

  private var result: StringBuilder = StringBuilder()

  private var dimensions: Int = 0

  private var seenTypeArgument: Boolean = false

  fun getResult(): String = result.toString() + "[]".repeat(dimensions)

  private val IGNORING_VISITOR: SignatureVisitor = object : SignatureVisitor(api) {}

  private fun convertBaseType(descriptor: Char): String = when (descriptor) {
    'V' -> "void"
    'B' -> "byte"
    'J' -> "long"
    'Z' -> "boolean"
    'I' -> "int"
    'S' -> "short"
    'C' -> "char"
    'F' -> "float"
    else -> "double"
  }

  override fun visitBaseType(baseType: Char) {
    result.append(convertBaseType(baseType))
  }

  override fun visitTypeVariable(typeVariable: String) {
    result.append(typeVariable)
  }

  override fun visitArrayType(): SignatureVisitor {
    dimensions++
    return this
  }

  override fun visitEnd() {
    if (seenTypeArgument) {
      result.append(">")
    }
  }

  override fun visitTypeArgument() {
    if (!seenTypeArgument) {
      seenTypeArgument = true
      result.append("<")
    } else {
      result.append(", ")
    }
    result.append("?")
  }

  override fun visitTypeArgument(kind: Char): SignatureVisitor {
    if (!seenTypeArgument) {
      seenTypeArgument = true
      result.append("<")
    } else {
      result.append(", ")
    }
    result.append("?")
    return IGNORING_VISITOR
  }

  override fun visitInnerClassType(name: String) {
    result.append(".").append(name)
  }

  override fun visitClassType(name: String) {
    result.append(binaryNameConverter(name))
  }

  //-----------

  private fun unsupported(): Nothing = throw IllegalStateException("This signature visitor is intended to parse only type signatures")

  override fun visitParameterType(): SignatureVisitor = unsupported()

  override fun visitFormalTypeParameter(p0: String?): Unit = unsupported()

  override fun visitClassBound(): SignatureVisitor = unsupported()

  override fun visitInterface(): SignatureVisitor = unsupported()

  override fun visitExceptionType(): SignatureVisitor = unsupported()

  override fun visitInterfaceBound(): SignatureVisitor = unsupported()

  override fun visitSuperclass(): SignatureVisitor = unsupported()

  override fun visitReturnType(): SignatureVisitor = unsupported()

}