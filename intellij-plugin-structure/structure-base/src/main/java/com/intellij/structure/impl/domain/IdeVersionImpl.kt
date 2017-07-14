package com.intellij.structure.impl.domain

import com.intellij.structure.ide.IdeVersion
import com.intellij.structure.impl.utils.StringUtil
import java.util.*

/**
 * @author Sergey Evdokimov
 */
class IdeVersionImpl private constructor(private val productCode: String, vararg components: Int, private val isSnapshot: Boolean = false) : IdeVersion() {
  private val myComponents = components

  override fun compareTo(other: IdeVersion): Int {
    if (other !is IdeVersionImpl) {
      if (baselineVersion != other.baselineVersion) {
        return baselineVersion - other.baselineVersion
      }
      if (build != other.build) {
        return build - other.build
      }
      if (isSnapshot != other.isSnapshot) {
        return if (isSnapshot) 1 else -1
      }
      return 0
    }

    val c1 = myComponents
    val c2 = other.myComponents

    for (i in 0..Math.min(c1.size, c2.size) - 1) {
      if (c1[i] == c2[i] && c1[i] == SNAPSHOT_VALUE) return 0
      if (c1[i] == SNAPSHOT_VALUE) return 1
      if (c2[i] == SNAPSHOT_VALUE) return -1

      val result = c1[i] - c2[i]
      if (result != 0) return result
    }
    return c1.size - c2.size
  }

  override fun asString(includeProductCode: Boolean, includeSnapshotMarker: Boolean): String {
    val builder = StringBuilder()

    if (includeProductCode && !StringUtil.isEmpty(productCode)) {
      builder.append(productCode).append('-')
    }

    builder.append(myComponents[0])
    for (i in 1..myComponents.size - 1) {
      if (myComponents[i] != SNAPSHOT_VALUE) {
        builder.append('.').append(myComponents[i])
      } else if (includeSnapshotMarker) {
        builder.append('.').append(if (isSnapshot) SNAPSHOT else STAR)
      }
    }

    return builder.toString()
  }

  override fun getComponents(): IntArray {
    return myComponents.clone()
  }

  override fun getBaselineVersion(): Int {
    return myComponents[0]
  }

  override fun getProductCode(): String = productCode

  override fun toString(): String {
    return asString()
  }

  override fun getBuild(): Int {
    return myComponents[1]
  }

  override fun isSnapshot(): Boolean {
    return isSnapshot
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false

    val that = other as IdeVersionImpl?

    return productCode == that!!.productCode &&
        isSnapshot == that.isSnapshot &&
        Arrays.equals(myComponents, that.myComponents)

  }

  override fun hashCode(): Int {
    var result = productCode.hashCode()
    result = 31 * result + Arrays.hashCode(myComponents)
    result = 31 * result + if (isSnapshot) 1 else 0
    return result
  }

  companion object {

    private val BUILD_NUMBER = "__BUILD_NUMBER__"
    private val STAR = "*"
    private val SNAPSHOT = "SNAPSHOT"
    private val FALLBACK_VERSION = "999.SNAPSHOT"
    private val SNAPSHOT_VALUE = Integer.MAX_VALUE

    @Throws(IllegalArgumentException::class)
    fun fromString(version: String): IdeVersionImpl {
      if (version.isBlank()) {
        throw IllegalArgumentException("Ide-version string must not be empty")
      }

      if (BUILD_NUMBER == version || SNAPSHOT == version) {
        val fallback = IdeVersionImpl.fromString(FALLBACK_VERSION)
        return IdeVersionImpl("", *fallback.myComponents, isSnapshot = true)
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

      val baselineVersionSeparator = code.indexOf('.')
      val baselineVersion: Int
      val buildNumber: Int

      if (baselineVersionSeparator > 0) {
        val baselineVersionString = code.substring(0, baselineVersionSeparator)
        if (baselineVersionString.trim { it <= ' ' }.isEmpty()) {
          throw IllegalArgumentException("Invalid version number: " + version)
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

        return IdeVersionImpl(productCode, *intComponents, isSnapshot = isSnapshot)

      } else {
        buildNumber = parseBuildNumber(version, code)

        if (buildNumber <= 2000) {
          // it's probably a baseline, not a build number
          return IdeVersionImpl(productCode, buildNumber, 0)
        }

        baselineVersion = getBaseLineForHistoricBuilds(buildNumber)
        return IdeVersionImpl(productCode, baselineVersion, buildNumber)
      }

    }

    private fun parseBuildNumber(version: String, code: String): Int {
      if (SNAPSHOT == code || STAR == code || BUILD_NUMBER == code) {
        return SNAPSHOT_VALUE
      }
      try {
        return Integer.parseInt(code)
      } catch (e: NumberFormatException) {
        throw IllegalArgumentException("Invalid version number: " + version)
      }

    }

    // See http://www.jetbrains.net/confluence/display/IDEADEV/Build+Number+Ranges for historic build ranges
    private fun getBaseLineForHistoricBuilds(bn: Int): Int {
      when {
        bn >= 10000 -> return 88 // Maia, 9x builds
        bn >= 9500 -> return 85 // 8.1 builds
        bn >= 9100 -> return 81 // 8.0.x builds
        bn >= 8000 -> return 80 // 8.0, including pre-release builds
        bn >= 7500 -> return 75 // 7.0.2+
        bn >= 7200 -> return 72 // 7.0 final
        bn >= 6900 -> return 69 // 7.0 pre-M2
        bn >= 6500 -> return 65 // 7.0 pre-M1
        bn >= 6000 -> return 60 // 6.0.2+
        bn >= 5000 -> return 55 // 6.0 branch, including all 6.0 EAP builds
        bn >= 4000 -> return 50 // 5.1 branch
        else -> return 40
      }
    }
  }
}
