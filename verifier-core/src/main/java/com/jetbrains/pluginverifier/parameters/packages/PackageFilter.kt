package com.jetbrains.pluginverifier.parameters.packages

/**
 * Determines if a class belongs to a set of packages
 * determined by [descriptors] [Descriptor].
 *
 * Each descriptor either includes or excludes a package.
 * A package is included by this filter iff:
 * 1) It is included by any descriptor.
 * 2) It is not excluded by any descriptor with
 * a package longer than the including one.
 *
 * For example, given a set of packages
 * * `+org.jetbrains.kotlin`
 * * `-org.jetbrains.kotlin.extension`
 * * `+org.jetbrains.kotlin.extension.included`
 *
 * the following classes
 *
 * * `org.jetbrains.kotlin.util.UtilKt` will be accepted
 * * `org.jetbrains.kotlin.extension.strings.StringsKt` will be rejected
 * * `org.jetbrains.kotlin.extension.included.IOUtils` will be accepted
 */
class PackageFilter(private val packages: List<Descriptor>) {

  /**
   * Returns `true` if a binary class [binaryClassName]
   * is accepted by this filter by rules described
   * in the [PackageFilter]'s docs.
   */
  fun accept(binaryClassName: String): Boolean {
    val longestIncluding = packages.asSequence()
        .filter { it.includeOrExclude && it.matchesPackageOf(binaryClassName) }
        .maxBy { it.binaryPackageName.length }
        ?: return false

    /**
     * Check that there is no excluding package with
     * a length bigger than the longest including one.
     */
    return packages.asSequence()
        .filter { !it.includeOrExclude && it.matchesPackageOf(binaryClassName) }
        .none { it.binaryPackageName.length >= longestIncluding.binaryPackageName.length }
  }

  private fun Descriptor.matchesPackageOf(binaryClassName: String) =
      binaryClassName.startsWith("$binaryPackageName/")

  /**
   * Descriptor of a package, used by the [PackageFilter].
   *
   * The descriptor either includes or excludes all classes from a package [binaryPackageName].
   */
  data class Descriptor(val includeOrExclude: Boolean, val binaryPackageName: String)

}