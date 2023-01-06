package com.jetbrains.plugin.structure.dotnet.version

import com.jetbrains.plugin.structure.base.utils.ReSharperCompatibilityUtils
import com.jetbrains.plugin.structure.base.utils.Version
import kotlin.math.min

data class ReSharperVersion(val components: List<Int>, override val productCode: String): Version<ReSharperVersion> {
  override fun compareTo(other: ReSharperVersion): Int {
   val compareProductCodes = productCode.compareTo(other.productCode)
    if (productCode.isNotEmpty() && other.productCode.isNotEmpty() && compareProductCodes != 0) {
      return compareProductCodes
    }
    val c1 = components
    val c2 = other.components
    for (i in 0 until min(c1.size, c2.size)) {
      val result = c1[i].compareTo(c2[i])
      if (result != 0) {
        return result
      }
    }
    return c1.size.compareTo(c2.size)
  }

  override fun asString() = asString(true)
  override fun asStringWithoutProductCode() = asString(false)
  override fun toString() = asString()
  fun asLong() = ReSharperCompatibilityUtils.versionAsLong(*components.toIntArray())

  companion object {
    fun fromString(versionString: String): ReSharperVersion {
      if (versionString.isBlank()) {
        throw IllegalArgumentException("Version string must not be empty")
      }
      val productSeparator = versionString.indexOf('-')
      val productCode: String
      var versionNumber = versionString
      if (productSeparator > 0) {
        productCode = versionString.substring(0, productSeparator)
        versionNumber = versionString.substring(productSeparator + 1)
      } else {
        productCode = ""
      }
      val components = versionNumber.trim().split('.').map { it.toIntOrNull() ?: throw IllegalArgumentException("Invalid version $versionNumber, should consists from numbers") }
      require(components.size >= 2) { "Invalid version number $versionNumber, should be at least 2 parts" }
      return ReSharperVersion(components, productCode)
    }
  }

  private fun asString(includeProductCode: Boolean): String {
    val builder = StringBuilder()

    if (includeProductCode && productCode.isNotEmpty()) {
      builder.append(productCode).append('-')
    }

    builder.append(components[0])
    for (i in 1 until components.size) {
      builder.append('.').append(components[i])
    }

    return builder.toString()
  }

  override fun setProductCodeIfAbsent(productCode: String) =
    if (this.productCode.isEmpty())
      fromString("$productCode-" + asStringWithoutProductCode())
    else {
      this
    }

  fun getLowerVersion(): ReSharperVersion {
    if (components.size >= 3 && components[2] != 0) {
      val minorValue = components[2]
      require(minorValue > 0) {
        "Current version has wrong format: minor value cannot be lower than 0"
      }
      return ReSharperVersion(listOf(this.components[0], this.components[1], minorValue - 1), this.productCode)
    }
    val baselineVersion = components.getOrElse(0) { 0 }
    val build = components.getOrElse(1) { 0 }
    if (build != 0) {
      require(build > 0) {
        "Current version has wrong format: build value cannot be lower than 0"
      }
      return ReSharperVersion(listOf(baselineVersion, build - 1, ReSharperCompatibilityUtils.getMaxMinor()), this.productCode)
    }
    require(baselineVersion > 0) {
      "Current version has wrong format: baseline value cannot be lower than 0"
    }
    return ReSharperVersion(listOf(baselineVersion - 1, ReSharperCompatibilityUtils.getMaxBuild()), this.productCode)
  }

  fun getHigherVersion(): ReSharperVersion {
    if (components.size < 3 || components[2] < Int.MAX_VALUE) {
      val minorValue = components.getOrElse(2) { 0 }
      return ReSharperVersion(listOf(components[0], components[1], minorValue + 1), this.productCode)
    }
    val baselineVersion = components.getOrElse(0) { 0 }
    val build = components.getOrElse(1) { 0 }
    if (build < Int.MAX_VALUE) {
      return ReSharperVersion(listOf(baselineVersion, build + 1), this.productCode)
    }
    require(baselineVersion < Int.MAX_VALUE) {
      "It is not possible to get higher version, since the current version is the maximum possible version"
    }
    return ReSharperVersion(listOf(baselineVersion + 1, 0), this.productCode)
  }
}
