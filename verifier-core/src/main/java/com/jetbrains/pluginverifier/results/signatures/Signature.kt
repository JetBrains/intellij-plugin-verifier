package com.jetbrains.pluginverifier.results.signatures

import com.jetbrains.pluginverifier.results.presentation.toFullJavaClassName

typealias Identifier = String

fun <T> Iterable<T>.tightJoin(): String = joinToString(separator = "")

fun StringBuilder.addSpaceIfNecessary() {
  if (isNotEmpty() && last() != ' ') {
    append(' ')
  }
}

fun <T> T?.toStringOrEmpty(): String = this?.toString() ?: ""

/*
 * This file contains declarations of classes representing nodes
 * of JVM generics signature parsing, as stated in
 * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.9.1
*/

data class FormatOptions(
    /**
     * Whether the type signature to be formatted relates to interface
     * rather than a class. If so, word "extends" will be used in respect
     * to implemented interfaces instead of "implements".
     */
    val isInterface: Boolean = false,

    /**
     * Whether generic type parameters of classes or methods must be printed.
     * ```
     * public class A<T> -> A<T>
     * public static <T> T foo() {} -> T foo
     * ```
     */
    val formalTypeParameters: Boolean = false,
    /**
     * Whether formal type parameters' bounds must be printed.
     * ```
     * public class A<T extends Number> -> A<T extends Number>
     * ```
     */
    val formalTypeParametersBounds: Boolean = false,

    /**
     * Whether generic type arguments of this top-level class
     * or inner classes must be printed.
     */
    val typeArguments: Boolean = false,

    /**
     * Whether super class of the class represented by this signature must be printed.
     */
    val superClass: Boolean = false,

    /**
     * Whether implemented interfaces of the class represented by this signature must be printed.
     */
    val superInterfaces: Boolean = false,

    /**
     * Whether exceptions thrown by the method represented by this signature must be printed.
     */
    val methodThrows: Boolean = false,

    /**
     * Function used to convert fully-qualified internal
     * class names to presentable names.
     *
     * By default [toFullJavaClassName] is used, which converts
     * names to Java fully-qualified names.
     * ```
     * org/some/Class -> org.some.Class
     * org/some/Nester$Class -> org.some.Nester.Class
     * org/some/Anonymous$1 -> org.some.Anonymous$1
     * ```
     */
    val internalNameConverter: (String) -> String = toFullJavaClassName,

    /**
     * Separator string used to separate several type
     * arguments of a reference type signature from each other.
     * ```
     * <T, K> -> <T, K>
     * ```
     */
    val typeArgumentsSeparator: String = ", ",
    /**
     * Separator string used to separate several type parameters
     * of a class signature from each other.
     * ```
     * public class A<T, K> -> <T, K>
     * ```
     */
    val typeParametersSeparator: String = ", "
) {
  /**
   * Converts internal class name to presentable name,
   * using specified [internalNameConverter].
   */
  fun convertClassName(className: String) = internalNameConverter(className)
}

/**
 * All signature nodes implement this interface and thus
 * allow to format them to presentable string according to [FormatOptions].
 *
 * [toString] for all signatures returns JVM-internal presentation
 * of this signature, that is the original signature.
 */
interface FormattableSignature {
  fun format(formatOptions: FormatOptions): String
}

/**
 * ```
 * JavaTypeSignature:
 *   ReferenceTypeSignature
 *   BaseType
 * ```
 */
sealed class JavaTypeSignature : FormattableSignature

/**
 * BaseType:
 *   B C D F I J S Z
 */
sealed class BaseType(private val char: Char) : JavaTypeSignature() {
  object B : BaseType('B')
  object J : BaseType('J')
  object Z : BaseType('Z')
  object I : BaseType('I')
  object S : BaseType('S')
  object C : BaseType('C')
  object F : BaseType('F')
  object D : BaseType('D')

  override fun format(formatOptions: FormatOptions): String =
      when (this) {
        BaseType.B -> "byte"
        BaseType.J -> "long"
        BaseType.Z -> "boolean"
        BaseType.I -> "int"
        BaseType.S -> "short"
        BaseType.C -> "char"
        BaseType.F -> "float"
        BaseType.D -> "double"
      }

  override fun toString() = char.toString()
}

/**
 * ```
 * ReferenceTypeSignature:
 *   ClassTypeSignature
 *   TypeVariableSignature
 *   ArrayTypeSignature
 * ```
 */
sealed class ReferenceTypeSignature : JavaTypeSignature()

/**
 * ```
 * ClassTypeSignature:
 *   L [PackageSpecifier] SimpleClassTypeSignature {ClassTypeSignatureSuffix} ;
 *
 * PackageSpecifier:
 *   Identifier / {PackageSpecifier}
 *
 * SimpleClassTypeSignature:
 *   Identifier [TypeArguments]
 *
 * ClassTypeSignatureSuffix:
 *   . SimpleClassTypeSignature
 * ```
 */
data class ClassTypeSignature(
    val topClassTypeSignature: SimpleClassTypeSignature,
    val innerClassTypeSignatures: List<SimpleClassTypeSignature>
) : ReferenceTypeSignature() {

  override fun format(formatOptions: FormatOptions) =
      buildString {
        append(topClassTypeSignature.format(formatOptions))
        for (innerClass in innerClassTypeSignatures) {
          append(".").append(innerClass.format(formatOptions))
        }
      }

  override fun toString() =
      buildString {
        append("L")
        append(topClassTypeSignature)
        for (suffix in innerClassTypeSignatures) {
          append(".").append(suffix)
        }
        append(";")
      }
}

/**
 * ```
 * TypeVariableSignature:
 *   T Identifier ;
 * ```
 */
data class TypeVariableSignature(val identifier: Identifier) : ReferenceTypeSignature() {
  override fun toString() = "T$identifier;"

  override fun format(formatOptions: FormatOptions) = identifier
}

/**
 * ```
 * ArrayTypeSignature:
 *   [ JavaTypeSignature
 * ```
 */
data class ArrayTypeSignature(
    val javaTypeSignature: JavaTypeSignature,
    val dimensions: Int
) : ReferenceTypeSignature() {
  override fun toString() = "[".repeat(dimensions) + "$javaTypeSignature"

  override fun format(formatOptions: FormatOptions) =
      javaTypeSignature.format(formatOptions) + "[]".repeat(dimensions)
}

/**
 * ```
 * SimpleClassTypeSignature:
 *   Identifier [TypeArguments]
 * ```
 */
data class SimpleClassTypeSignature(
    val identifier: Identifier,
    val typeArguments: TypeArguments?
) : FormattableSignature {
  override fun toString() = identifier + typeArguments.toStringOrEmpty()

  override fun format(formatOptions: FormatOptions): String =
      buildString {
        append(formatOptions.convertClassName(identifier))
        if (formatOptions.typeArguments && typeArguments != null) {
          append(typeArguments.format(formatOptions))
        }
      }
}

/**
 * ```
 * TypeArguments:
 *   < TypeArgument {TypeArgument} >
 * ```
 */
data class TypeArguments(val typeArguments: List<TypeArgument>) : FormattableSignature {
  override fun toString() = "<" + typeArguments.tightJoin() + ">"

  override fun format(formatOptions: FormatOptions) =
      "<" + typeArguments.joinToString(separator = formatOptions.typeArgumentsSeparator) { it.format(formatOptions) } + ">"
}

/**
 * ```
 * TypeArgument:
 *   [WildcardIndicator] ReferenceTypeSignature
 *   *
 * ```
 */
sealed class TypeArgument : FormattableSignature {
  object Any : TypeArgument() {
    override fun toString() = "*"

    override fun format(formatOptions: FormatOptions) = "?"
  }

  data class RefType(
      val wildcardIndicator: WildcardIndicator?,
      val referenceTypeSignature: ReferenceTypeSignature
  ) : TypeArgument() {

    override fun toString() = when (wildcardIndicator) {
      WildcardIndicator.PLUS -> "+"
      WildcardIndicator.MINUS -> "-"
      null -> ""
    } + "$referenceTypeSignature"

    override fun format(formatOptions: FormatOptions) =
        when (wildcardIndicator) {
          WildcardIndicator.PLUS -> "? extends "
          WildcardIndicator.MINUS -> "? super "
          null -> ""
        } + referenceTypeSignature.format(formatOptions)
  }
}


/**
 * ```
 * WildcardIndicator:
 *   +
 *   -
 * ```
 */
enum class WildcardIndicator {
  PLUS, MINUS
}

/**
 * ```
 * ClassSignature:
 *   [TypeParameters] SuperclassSignature {SuperinterfaceSignature}
 * ```
 */
data class ClassSignature(
    val typeParameters: TypeParameters?,
    val superclassSignature: ClassTypeSignature,
    val superinterfaceSignatures: List<ClassTypeSignature>
) : FormattableSignature {

  override fun toString() = typeParameters.toStringOrEmpty() +
      "$superclassSignature" + superinterfaceSignatures.tightJoin()

  override fun format(formatOptions: FormatOptions) =
      buildString {
        if (formatOptions.formalTypeParameters && typeParameters != null) {
          append(typeParameters.format(formatOptions))
        }
        if (formatOptions.superClass && superclassSignature.topClassTypeSignature.identifier != "java/lang/Object") {
          addSpaceIfNecessary()
          append("extends ")
          append(superclassSignature.format(formatOptions))
        }
        if (formatOptions.superInterfaces && superinterfaceSignatures.isNotEmpty()) {
          addSpaceIfNecessary()
          if (formatOptions.isInterface) {
            append("extends ")
          } else {
            append("implements ")
          }
          append(superinterfaceSignatures.joinToString { it.format(formatOptions) })
        }
      }
}

/**
 * ```
 * TypeParameters:
 *   < TypeParameter {TypeParameter} >
 * ```
 */
data class TypeParameters(val typeParameters: List<TypeParameter>) : FormattableSignature {
  override fun toString() = "<" + typeParameters.tightJoin() + ">"

  override fun format(formatOptions: FormatOptions) =
      "<" + typeParameters.joinToString(separator = formatOptions.typeParametersSeparator) { it.format(formatOptions) } + ">"
}

/**
 *```
 *  TypeParameter:
 *    Identifier ClassBound {InterfaceBound}
 *
 *  ClassBound:
 *   : [ReferenceTypeSignature]
 *
 *  InterfaceBound:
 *   : ReferenceTypeSignature
 *```
 */
data class TypeParameter(
    val identifier: Identifier,
    val classBound: ReferenceTypeSignature?,
    val interfaceBounds: List<ReferenceTypeSignature>
) : FormattableSignature {
  override fun toString() =
      buildString {
        append(identifier)
        append(":").append(classBound.toStringOrEmpty())
        for (interfaceBound in interfaceBounds) {
          append(":").append(interfaceBound)
        }
      }

  override fun format(formatOptions: FormatOptions): String =
      buildString {
        append(identifier)
        if (formatOptions.formalTypeParametersBounds) {
          val needSuper = classBound != null && !(classBound is ClassTypeSignature && classBound.topClassTypeSignature.identifier == "java/lang/Object")
          if (needSuper) {
            append(" extends ").append(classBound!!.format(formatOptions))
          }
          if (interfaceBounds.isNotEmpty()) {
            if (needSuper) {
              append(", ")
            } else {
              append(" extends ")
            }
            append(interfaceBounds.joinToString { it.format(formatOptions) })
          }
        }
      }
}

/**
 * ```
 * MethodSignature:
 *   [TypeParameters] ( {JavaTypeSignature} ) Result {ThrowsSignature}
 * ```
 */
data class MethodSignature(
    val typeParameters: TypeParameters?,
    val parameterSignatures: List<JavaTypeSignature>,
    val result: Result,
    val throwsSignatures: List<ThrowsSignature>
) : FormattableSignature {
  override fun format(formatOptions: FormatOptions): String {
    return buildString {
      if (formatOptions.formalTypeParameters && typeParameters != null) {
        append(typeParameters.format(formatOptions))
        append(" ")
      }
      append(result.format(formatOptions))
      append("(")
      append(parameterSignatures.joinToString { it.format(formatOptions) })
      append(")")
      if (formatOptions.methodThrows && throwsSignatures.isNotEmpty()) {
        append(" throws ")
        append(throwsSignatures.joinToString { it.format(formatOptions) })
      }
    }
  }

  override fun toString() = typeParameters.toStringOrEmpty() +
      "(" + parameterSignatures.tightJoin() + ")" + result.toString() + throwsSignatures.tightJoin()
}

/**
 * ```
 * Result:
 *   JavaTypeSignature
 *   VoidDescriptor
 * ```
 */
sealed class Result : FormattableSignature {
  data class JavaType(val javaTypeSignature: JavaTypeSignature) : Result() {
    override fun toString() = javaTypeSignature.toString()

    override fun format(formatOptions: FormatOptions) =
        javaTypeSignature.format(formatOptions)
  }

  object VoidDescriptor : Result() {
    override fun toString() = "V"

    override fun format(formatOptions: FormatOptions) = "void"
  }
}

/**
 * ThrowsSignature:
 *   ^ ClassTypeSignature
 *   ^ TypeVariableSignature
 */
sealed class ThrowsSignature : FormattableSignature {
  data class ClassType(val classTypeSignature: ClassTypeSignature) : ThrowsSignature() {
    override fun toString() = "^$classTypeSignature"

    override fun format(formatOptions: FormatOptions) =
        classTypeSignature.format(formatOptions)
  }

  data class TypeVar(val typeVariableSignature: TypeVariableSignature) : ThrowsSignature() {
    override fun toString() = "^$typeVariableSignature"

    override fun format(formatOptions: FormatOptions) =
        typeVariableSignature.format(formatOptions)
  }
}

/**
 * ```
 * FieldSignature:
 *   ReferenceTypeSignature
 * ```
 */
data class FieldSignature(val referenceTypeSignature: ReferenceTypeSignature) : FormattableSignature {
  override fun toString() = "$referenceTypeSignature"

  override fun format(formatOptions: FormatOptions) = referenceTypeSignature.format(formatOptions)
}