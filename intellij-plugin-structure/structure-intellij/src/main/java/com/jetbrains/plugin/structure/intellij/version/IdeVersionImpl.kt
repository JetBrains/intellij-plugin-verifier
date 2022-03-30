/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.version

import com.jetbrains.plugin.structure.base.utils.CompatibilityUtils
import java.util.*

class IdeVersionImpl(
  override val productCode: String,
  private val components: IntArray,
  private val isSnapshot: Boolean = false
) : IdeVersion() {

  override fun asString(includeProductCode: Boolean, includeSnapshotMarker: Boolean): String {
    val builder = StringBuilder()

    if (includeProductCode && productCode.isNotEmpty()) {
      builder.append(productCode).append('-')
    }

    builder.append(components[0])
    for (i in 1 until components.size) {
      if (components[i] != SNAPSHOT_VALUE) {
        builder.append('.').append(components[i])
      } else if (includeSnapshotMarker) {
        builder.append('.').append(if (isSnapshot) SNAPSHOT else STAR)
      }
    }

    return builder.toString()
  }

  override fun setProductCodeIfAbsent(productCode: String) =
    if (this.productCode.isEmpty())
      fromString("$productCode-" + asStringWithoutProductCode())
    else {
      this
    }

  override fun getComponents() = components.clone()

  override fun asLong(): Long = CompatibilityUtils.versionAsLong(*components)

  override fun getBaselineVersion() = components[0]

  override fun toString() = asString()

  override fun getBuild() = components[1]

  override fun isSnapshot() = isSnapshot

  companion object {

    private const val BUILD_NUMBER = "__BUILD_NUMBER__"
    private const val STAR = "*"
    private const val SNAPSHOT = "SNAPSHOT"
    private const val FALLBACK_VERSION = "999.SNAPSHOT"
    private const val SNAPSHOT_VALUE = Integer.MAX_VALUE

    @Throws(IllegalArgumentException::class)
    fun fromString(version: String): IdeVersionImpl {
      if (version.isBlank()) {
        throw IllegalArgumentException("Ide-version string must not be empty")
      }

      if (BUILD_NUMBER == version || SNAPSHOT == version) {
        val fallback = fromString(FALLBACK_VERSION)
        return IdeVersionImpl("", fallback.components, isSnapshot = true)
      }

      var code = version
      val productSeparator = code.lastIndexOf('-') //some products have multiple parts, e.g. "FB-IC-143.157"
      val productCode: String
      if (productSeparator > 0) {
        productCode = code.substring(0, productSeparator)
        code = code.substring(productSeparator + 1)
      } else {
        productCode = ""
      }

      validateProductCode(version, productCode)

      val baselineVersionSeparator = code.indexOf('.')
      val baselineVersion: Int
      val buildNumber: Int

      if (baselineVersionSeparator > 0) {
        val baselineVersionString = code.substring(0, baselineVersionSeparator)
        if (baselineVersionString.trim { it <= ' ' }.isEmpty()) {
          throw IllegalArgumentException("Invalid version number: $version")
        }

        val components = code.split('.')
        val intComponentsList = ArrayList<Int>()

        var isSnapshot = false
        for (component in components) {
          val comp = parseBuildNumber(version, component)
          intComponentsList.add(comp)
          if (comp == SNAPSHOT_VALUE) {
            if (component == SNAPSHOT) isSnapshot = true
            break
          }
        }

        if (intComponentsList.size < 2) {
          throw IllegalArgumentException("Invalid version number $version: build not specified")
        }

        val intComponents = IntArray(intComponentsList.size)
        for (i in intComponentsList.indices) {
          intComponents[i] = intComponentsList[i]
        }

        return IdeVersionImpl(productCode, intComponents, isSnapshot = isSnapshot)

      } else {
        buildNumber = parseBuildNumber(version, code)

        if (buildNumber <= 2000) {
          // it's probably a baseline, not a build number
          return IdeVersionImpl(productCode, intArrayOf(buildNumber, 0))
        }

        baselineVersion = getBaseLineForHistoricBuilds(buildNumber)
        return IdeVersionImpl(productCode, intArrayOf(baselineVersion, buildNumber))
      }

    }

    /**
     * Valid product codes:
     * - IU
     * - FB-IC
     * - A-B-C
     */
    private fun validateProductCode(version: String, productCode: String) {
      if (productCode.isNotEmpty()) {
        for (c in productCode) {
          if (c != '-' && !c.isLetter()) {
            throw IllegalArgumentException("Invalid character '$c' in product code: $version")
          }
        }
      }
      if (version.startsWith("-") || version.endsWith("-") || version.contains("--")) {
        throw IllegalArgumentException("Invalid product code: $version")
      }
    }

    private fun parseBuildNumber(version: String, code: String): Int {
      if (SNAPSHOT == code || STAR == code || BUILD_NUMBER == code) {
        return SNAPSHOT_VALUE
      }
      try {
        return Integer.parseInt(code)
      } catch (e: NumberFormatException) {
        throw IllegalArgumentException("Invalid version number: $version")
      }

    }

    // See https://www.jetbrains.net/confluence/display/IDEADEV/Build+Number+Ranges for historic build ranges
    private fun getBaseLineForHistoricBuilds(bn: Int) =
      when {
        bn >= 10000 -> 88 // Maia, 9x builds
        bn >= 9500 -> 85 // 8.1 builds
        bn >= 9100 -> 81 // 8.0.x builds
        bn >= 8000 -> 80 // 8.0, including pre-release builds
        bn >= 7500 -> 75 // 7.0.2+
        bn >= 7200 -> 72 // 7.0 final
        bn >= 6900 -> 69 // 7.0 pre-M2
        bn >= 6500 -> 65 // 7.0 pre-M1
        bn >= 6000 -> 60 // 6.0.2+
        bn >= 5000 -> 55 // 6.0 branch, including all 6.0 EAP builds
        bn >= 4000 -> 50 // 5.1 branch
        else -> 40
      }
  }
}
