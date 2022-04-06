package com.jetbrains.plugin.structure.dotnet.version

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
}

data class WaveVersion(val firstComponent: Int, val secondComponent: Int, override val productCode: String = ""): Version<WaveVersion> {
  override fun compareTo(other: WaveVersion) = compareValuesBy(this, other, {it.firstComponent}, {it.secondComponent})
  override fun asString() = "$firstComponent.$secondComponent"
  override fun asStringWithoutProductCode() = asString()

  companion object {
    fun fromString(versionString: String) : WaveVersion {
      val versionStringComponents = versionString.split('.')
      val versionFirstComponent = versionStringComponents[0].toIntOrNull()
        ?: throw IllegalArgumentException("Cannot parse the first component of version")
      val minVersionSecondComponent = if (versionStringComponents.size > 1) versionStringComponents[1].toIntOrNull()
        ?: throw IllegalArgumentException("Cannot parse the second component of version") else 0
      return WaveVersion(versionFirstComponent, minVersionSecondComponent)
    }
  }

  // Wave versions can't have product codes
  override fun setProductCodeIfAbsent(productCode: String): WaveVersion = this
}
