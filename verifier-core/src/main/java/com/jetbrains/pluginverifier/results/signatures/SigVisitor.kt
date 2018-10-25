package com.jetbrains.pluginverifier.results.signatures

import com.jetbrains.pluginverifier.verifiers.ASM_API_LEVEL
import org.objectweb.asm.signature.SignatureVisitor


/**
 * A visitor to visit a generic signature. The methods of this interface must be called in one of
 * the three following orders (the last one is the only valid order for a [SigVisitor]
 * that is returned by a method of this class.
 * ```
 *  ClassSignature = ( visitFormalTypeParameter visitClassBound? visitInterfaceBound* )* (visitSuperclass visitInterface* )
 *  MethodSignature = ( visitFormalTypeParameter visitClassBound? visitInterfaceBound* )* (visitParameterType* visitReturnType visitExceptionType* )
 *  TypeSignature = visitBaseType | visitTypeVariable | visitArrayType | ( visitClassType visitTypeArgument* ( visitInnerClassType visitTypeArgument* )* visitEnd ) )
 * ```
 */
class SigVisitor : SignatureVisitor {

  private companion object {
    val UNBOUNDED_CLASS_TYPE_ARGUMENT_INDICATOR = SigVisitor()
  }

  constructor() : super(ASM_API_LEVEL) {
    formalTypeParameterName = null
  }

  private constructor(formalTypeParameterName: String) : super(ASM_API_LEVEL) {
    this.formalTypeParameterName = formalTypeParameterName
  }

  /**
   * Name of the formal type parameter being visited,
   * or `null` if the current visitor is not visiting any formal type parameters.
   */
  private val formalTypeParameterName: String?

  /**
   * Visitors used to parse formal type parameters of a class, interface or method.
   */
  private val formalTypeParameterVisitors = mutableListOf<SigVisitor>()

  /**
   * Visitor used to parse class bound of the current formal type parameter.
   */
  private var classBoundVisitor: SigVisitor? = null

  /**
   * Visitors used to parse interfaces bounds of the current formal type parameter.
   */
  private val interfaceBoundVisitors = mutableListOf<SigVisitor>()

  /**
   * Visitors used to parse method's parameters' signatures.
   */
  private val paramVisitors = mutableListOf<SigVisitor>()

  /**
   * Visitors used to parse signatures of method's thrown exceptions.
   */
  private val exceptionVisitors = mutableListOf<SigVisitor>()

  /**
   * Visitor used to parse signature of the super class of this class signature.
   */
  private var superClassVisitor: SigVisitor? = null

  /**
   * Visitors used to parse signatures of implemented interfaces
   * of this class signature.
   */
  private val interfaceVisitors = mutableListOf<SigVisitor>()

  /**
   * Visitor used to parse signature of the method's return type.
   */
  private var returnTypeVisitor: SigVisitor? = null

  /**
   * Number of array dimensions of this class' signature.
   */
  private var arrayDimensions = 0

  /**
   * Character corresponding to base type represented by this type signature.
   */
  private var baseType: Char? = null

  /**
   * Type variable name corresponding to this type signature.
   */
  private var typeVariable: Identifier? = null

  /**
   * Contains parts of the current class name being parsed in the type signature.
   *
   * The first part is the top-level class' name,
   * and the later parts, if any, are local inner class names.
   *
   * Type arguments corresponding to each inner class can be found
   * in [classTypeArgumentsVisitors] by the same indices.
   */
  private var classNameParts = mutableListOf<Identifier>()

  /**
   * Visitors used to parse type arguments of top-level and
   * inner classes being visited by this visitor.
   *
   * Corresponding classes' names can be found in [classNameParts]
   * by the same indices.
   */
  private val classTypeArgumentsVisitors = mutableListOf<MutableList<SigVisitor>>()

  private var wildcardIndicator: WildcardIndicator? = null

  override fun visitBaseType(descriptor: Char) {
    baseType = descriptor
  }

  override fun visitParameterType(): SignatureVisitor =
      SigVisitor().also { paramVisitors += it }

  override fun visitFormalTypeParameter(name: String) {
    SigVisitor(name).also { formalTypeParameterVisitors += it }
  }

  override fun visitClassBound(): SignatureVisitor {
    /**
     * Return the visitor created in [visitFormalTypeParameter],
     * which is used to parse class and interface bounds of this
     * formal type parameter
     */
    val lastVisitor = formalTypeParameterVisitors.last()
    return SigVisitor().also { lastVisitor.classBoundVisitor = it }
  }

  override fun visitInterfaceBound(): SignatureVisitor {
    /**
     * Return the visitor created in [visitFormalTypeParameter],
     * which is used to parse class and interface bounds of this
     * formal type parameter
     */
    val lastVisitor = formalTypeParameterVisitors.last()
    return SigVisitor().also { lastVisitor.interfaceBoundVisitors += it }
  }

  override fun visitInterface(): SignatureVisitor =
      SigVisitor().also { interfaceVisitors += it }

  override fun visitTypeVariable(name: String) {
    typeVariable = name
  }

  override fun visitExceptionType(): SignatureVisitor =
      SigVisitor().also { exceptionVisitors += it }

  override fun visitArrayType(): SignatureVisitor {
    arrayDimensions++
    return this
  }

  override fun visitSuperclass(): SignatureVisitor =
      SigVisitor().also { superClassVisitor = it }

  override fun visitReturnType(): SignatureVisitor =
      SigVisitor().also { returnTypeVisitor = it }

  override fun visitClassType(name: String) {
    //Adds class name part and container for type arguments' visitors.
    classNameParts.add(name)
    classTypeArgumentsVisitors.add(mutableListOf())
  }

  override fun visitInnerClassType(name: String) {
    //Adds class name part and container for type arguments' visitors.
    classNameParts.add(name)
    classTypeArgumentsVisitors.add(mutableListOf())
  }

  override fun visitTypeArgument() {
    classTypeArgumentsVisitors.last().add(UNBOUNDED_CLASS_TYPE_ARGUMENT_INDICATOR)
  }

  override fun visitTypeArgument(wildcard: Char): SignatureVisitor =
      SigVisitor().also {
        it.wildcardIndicator = if (wildcard == '+') {
          WildcardIndicator.PLUS
        } else if (wildcard == '-') {
          WildcardIndicator.MINUS
        } else {
          null
        }
        classTypeArgumentsVisitors.last().add(it)
      }

  override fun visitEnd() = Unit

  fun getClassSignature(): ClassSignature =
      ClassSignature(
          getTypeParameters(),
          superClassVisitor!!.getClassTypeSignature(),
          interfaceVisitors.map { it.getClassTypeSignature() }
      )

  fun getMethodSignature(): MethodSignature =
      MethodSignature(
          getTypeParameters(),
          paramVisitors.map { it.getJavaTypeSignature() },
          returnTypeVisitor!!.getResult(),
          exceptionVisitors.map { it.getThrowsSignature() }
      )

  fun getFieldSignature(): FieldSignature =
      FieldSignature(getReferenceTypeSignature())

  private fun getJavaTypeSignature(): JavaTypeSignature =
      if (baseType != null) {
        getBaseType()
      } else {
        getReferenceTypeSignature()
      }

  private fun getBaseType() =
      when (baseType) {
        'B' -> BaseType.B
        'J' -> BaseType.J
        'Z' -> BaseType.Z
        'I' -> BaseType.I
        'S' -> BaseType.S
        'C' -> BaseType.C
        'F' -> BaseType.F
        'D' -> BaseType.D
        else -> throw IllegalArgumentException("$baseType")
      }

  private fun getTypeParameter(): TypeParameter =
      TypeParameter(
          formalTypeParameterName!!,
          classBoundVisitor?.getReferenceTypeSignature(),
          interfaceBoundVisitors.map { it.getReferenceTypeSignature() }
      )

  private fun getTypeParameters(): TypeParameters? =
      formalTypeParameterVisitors
          .map { it.getTypeParameter() }
          .takeIf { it.isNotEmpty() }
          ?.let { TypeParameters(it) }

  private fun getTypeVariableSignature(): TypeVariableSignature =
      TypeVariableSignature(typeVariable!!)

  private fun getThrowsSignature(): ThrowsSignature =
      if (typeVariable != null) {
        ThrowsSignature.TypeVar(getTypeVariableSignature())
      } else {
        ThrowsSignature.ClassType(getClassTypeSignature())
      }

  private fun getResult(): Result =
      if (baseType == 'V') {
        Result.VoidDescriptor
      } else {
        Result.JavaType(getJavaTypeSignature())
      }

  private fun ReferenceTypeSignature.maybeArray(): ReferenceTypeSignature =
      if (arrayDimensions > 0) getArrayTypeSignature() else this

  private fun JavaTypeSignature.getArrayTypeSignature() =
      ArrayTypeSignature(this, arrayDimensions).also { check(arrayDimensions > 0) }

  private fun getReferenceTypeSignature(): ReferenceTypeSignature =
      if (typeVariable != null) {
        getTypeVariableSignature().maybeArray()
      } else if (baseType != null) {
        //If this is a base type, this method invocation must return array type.
        getBaseType().getArrayTypeSignature()
      } else {
        getClassTypeSignature().maybeArray()
      }

  private fun getClassTypeSignature(): ClassTypeSignature {
    check(classNameParts.isNotEmpty())

    val topClassName = classNameParts.first()
    val topClassTypeArgumentsVisitors = classTypeArgumentsVisitors.first()

    val innerClassTypeSignatures = classNameParts.indices
        .drop(1)
        .map { index ->
          createSimpleClassTypeSignature(
              classNameParts[index],
              classTypeArgumentsVisitors[index]
          )
        }

    val topClassTypeSignature = createSimpleClassTypeSignature(topClassName, topClassTypeArgumentsVisitors)
    return ClassTypeSignature(topClassTypeSignature, innerClassTypeSignatures)
  }

  private fun createSimpleClassTypeSignature(
      identifier: Identifier,
      typeArgumentVisitors: List<SigVisitor>
  ) = SimpleClassTypeSignature(identifier, typeArgumentVisitors.createTypeArguments())

  private fun List<SigVisitor>.createTypeArguments(): TypeArguments? {
    val typeArguments = map {
      if (it === UNBOUNDED_CLASS_TYPE_ARGUMENT_INDICATOR) {
        TypeArgument.Any
      } else {
        TypeArgument.RefType(it.wildcardIndicator, it.getReferenceTypeSignature())
      }
    }
    return if (typeArguments.isEmpty()) {
      null
    } else {
      TypeArguments(typeArguments)
    }
  }
}
