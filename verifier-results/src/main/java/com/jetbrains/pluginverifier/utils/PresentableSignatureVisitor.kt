package com.jetbrains.pluginverifier.utils

import org.objectweb.asm.Opcodes
import org.objectweb.asm.signature.SignatureVisitor

/*
These are the complete signature rules:

JavaTypeSignature:
  ReferenceTypeSignature
  BaseType

BaseType:
  B C D F I J S Z

ReferenceTypeSignature:
  ClassTypeSignature
  TypeVariableSignature
  ArrayTypeSignature

ClassTypeSignature:
  L [PackageSpecifier] SimpleClassTypeSignature {ClassTypeSignatureSuffix} ;

PackageSpecifier:
  Identifier / {PackageSpecifier}

SimpleClassTypeSignature:
  Identifier [TypeArguments]

TypeArguments:
  < TypeArgument {TypeArgument} >

TypeArgument:
  [WildcardIndicator] ReferenceTypeSignature
  *

WildcardIndicator:
  +
  -

ClassTypeSignatureSuffix:
  . SimpleClassTypeSignature

TypeVariableSignature:
  T Identifier ;

ArrayTypeSignature:
  [ JavaTypeSignature

-----------------------
ClassSignature:
  [TypeParameters] SuperclassSignature {SuperinterfaceSignature}

TypeParameters:
  < TypeParameter {TypeParameter} >

TypeParameter:
  Identifier ClassBound {InterfaceBound}

ClassBound:
  : [ReferenceTypeSignature]

InterfaceBound:
  : ReferenceTypeSignature

SuperclassSignature:
  ClassTypeSignature

SuperinterfaceSignature:
  ClassTypeSignature

-----------------------
MethodSignature:
  [TypeParameters] ( {JavaTypeSignature} ) Result {ThrowsSignature}

Result:
  JavaTypeSignature
  VoidDescriptor

ThrowsSignature:
  ^ ClassTypeSignature
  ^ TypeVariableSignature

VoidDescriptor:
  V

-----------------------
FieldSignature:
  ReferenceTypeSignature
*/
class PresentableSignatureVisitor(val binaryNameConverter: (String) -> String) : SignatureVisitor(Opcodes.ASM5) {

  private val IGNORING_VISITOR = object : SignatureVisitor(api) {}

  private val formalTypeParameters: StringBuilder = StringBuilder()

  private var seenFormalParameter: Boolean = false

  private var returnType: TypeSignatureVisitor? = null

  private val methodParameters: MutableList<TypeSignatureVisitor> = arrayListOf()

  override fun visitFormalTypeParameter(name: String) {
    if (!seenFormalParameter) {
      formalTypeParameters.append("<")
      seenFormalParameter = true
    } else {
      formalTypeParameters.append(", ")
    }
    formalTypeParameters.append(name)
  }

  override fun visitClassBound(): SignatureVisitor = IGNORING_VISITOR

  override fun visitInterfaceBound(): SignatureVisitor = IGNORING_VISITOR

  override fun visitInterface(): SignatureVisitor = IGNORING_VISITOR

  override fun visitParameterType(): SignatureVisitor {
    endFormals()
    val parameterVisitor = TypeSignatureVisitor(binaryNameConverter)
    methodParameters.add(parameterVisitor)
    return parameterVisitor
  }

  override fun visitSuperclass(): SignatureVisitor {
    endFormals()
    return IGNORING_VISITOR
  }

  override fun visitReturnType(): SignatureVisitor {
    endFormals()
    returnType = TypeSignatureVisitor(binaryNameConverter)
    return returnType!!
  }

  override fun visitExceptionType(): SignatureVisitor = IGNORING_VISITOR

  override fun visitTypeVariable(name: String): Unit = unsupported()

  override fun visitArrayType(): SignatureVisitor = unsupported()

  override fun visitClassType(name: String): Unit = unsupported()

  override fun visitBaseType(baseType: Char): Unit = unsupported()

  override fun visitTypeArgument(): Unit = unsupported()

  override fun visitTypeArgument(kind: Char): SignatureVisitor = unsupported()

  override fun visitInnerClassType(name: String?): Unit = unsupported()

  override fun visitEnd(): Unit = unsupported()

  private fun unsupported(): Nothing = throw IllegalStateException("This signature visitor is not intended to parse type signatures")

  fun getClassFormalTypeParameters(): String = formalTypeParameters.toString()

  fun getReturnType(): String = returnType?.getResult() ?: throw IllegalArgumentException("Exptected method signature")

  fun getMethodParameterTypes(): List<String> = methodParameters.map { it.getResult() }

  // -----------------------------------------------

  private fun endFormals() {
    if (seenFormalParameter) {
      formalTypeParameters.append('>')
    }
  }
}

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