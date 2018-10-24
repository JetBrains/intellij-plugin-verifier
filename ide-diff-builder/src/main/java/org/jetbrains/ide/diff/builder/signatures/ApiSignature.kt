package org.jetbrains.ide.diff.builder.signatures

/**
 * Base class for signatures to be recorded in API snapshots.
 *
 * These signatures, when serialized through [externalPresentation],
 * can be read by external annotations manager on IDE side.
 */
sealed class ApiSignature {
  /**
   * Returns name of the package that this element belongs to.
   */
  abstract val packageName: String

  /**
   * Converts internal presentation of API to string presentation
   * expected by external annotations system.
   * In IDEA codebase the same conversion from PSI element to string is done
   * in [com.intellij.psi.util.PsiFormatUtil.getExternalName].
   *
   * Examples of external names:
   * ```
   * pkg.A
   * pkg.A A()
   * pkg.A A(java.lang.String)
   * pkg.A void m1()
   * pkg.A void m2(java.util.List<java.lang.String>)
   * pkg.A T m3()
   * pkg.A T m4(java.util.Map<java.lang.String,java.lang.Object>)
   * pkg.A field
   * ```
   * For more conversion examples refer to unit tests of this method.
   */
  abstract val externalPresentation: String

  final override fun toString() = externalPresentation
}

/**
 * Signature of an API class, consisting of its fully qualified name.
 */
data class ClassSignature(
    override val packageName: String,
    val className: String
) : ApiSignature() {

  override val externalPresentation: String
    get() = className
}

/**
 * Signature of an API method, consisting of the host class' fully qualified name,
 * method name, and generified signatures of the parameters and return type.
 */
data class MethodSignature(
    override val packageName: String,
    val hostClass: String,
    val methodName: String,
    val returnType: String?,
    val paramsSignature: String
) : ApiSignature() {

  override val externalPresentation: String
    get() = hostClass +
        if (returnType != null) {
          " $returnType"
        } else {
          ""
        } + " $methodName($paramsSignature)"
}

/**
 * Signature of an API field, consisting of the host class' fully qualified name
 * and field name.
 */
data class FieldSignature(
    override val packageName: String,
    val hostClass: String,
    val fieldName: String
) : ApiSignature() {

  override val externalPresentation: String
    get() = "$hostClass $fieldName"
}
