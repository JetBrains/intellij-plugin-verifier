/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.packages

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
class DefaultPackageFilter(private val packages: List<Descriptor>) : PackageFilter {

  override fun acceptPackageOfClass(binaryClassName: String): Boolean {
    val longestIncluding = packages.asSequence()
      .filter { it.includeOrExclude && it.matchesPackageOf(binaryClassName) }
      .maxByOrNull { it.binaryPackageName.length }
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
   * Descriptor of a package, used by the [DefaultPackageFilter].
   *
   * The descriptor either includes or excludes all classes from a package [binaryPackageName].
   */
  data class Descriptor(val includeOrExclude: Boolean, val binaryPackageName: String)

}