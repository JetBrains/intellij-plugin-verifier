package org.jetbrains.ide.diff.builder.filter

object NonImplementationClassFilter : ClassFilter {

  /**
   * Returns `true` if this class is likely an implementation of something.
   * `org.some.ServiceImpl` -> true
   * `org.some.InterfaceImpl.InnerClass` -> true
   */
  private fun hasImplementationLikeName(className: String): Boolean {
    val simpleName = className.substringAfterLast("/")
    return simpleName.endsWith("Impl") || className.contains("Impl\$")
  }

  /**
   * Returns `true` if this package is likely a package containing implementation of some APIs.
   * `org.some.impl.services` -> true
   */
  private fun hasImplementationLikePackage(className: String): Boolean = "/impl/" in className

  override fun shouldProcessClass(className: String) =
      !hasImplementationLikeName(className) && !hasImplementationLikePackage(className)
}