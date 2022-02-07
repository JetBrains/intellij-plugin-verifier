package com.jetbrains.plugin.structure.dotnet.version

import com.jetbrains.plugin.structure.base.utils.Version

data class ReSharperVersion(val baseline: Int, val build: Int, val minor: Int? = null, val productCode: String = ""): Version<ReSharperVersion> {
  override fun compareTo(other: ReSharperVersion) = compareValuesBy(this, other, {it.baseline}, {it.build}, {it.minor})
  override fun asString() = asString(true)
  override fun asStringWithoutProductCode() = asString(false)
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
      val components = versionNumber.trim().split('.')
      require(components.size >= 2) { "Invalid version number $versionNumber, should be at least 2 parts" }
      val (baseline, build) = Pair (components[0].toIntOrNull(), components[1].toIntOrNull())
      require(baseline != null && build != null) { "Invalid version $versionNumber, should consists from numbers" }
      val minor = if (components.size > 2) components[2].toIntOrNull()
        ?: throw IllegalArgumentException("Invalid version $versionNumber, the third component should be number") else null
      return ReSharperVersion(baseline, build, minor, productCode)
    }
  }
  private fun asString(includeProductCode: Boolean) = (if (includeProductCode && productCode != "") "$productCode-" else "") +
    "$baseline.$build" + if (minor != null) ".$minor" else ""
}

data class WaveVersion(val firstComponent: Int, val secondComponent: Int): Version<WaveVersion> {
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
}
