package com.jetbrains.plugin.structure.base.utils

abstract class BaseCompatibilityUtils {
  abstract val maxBranchValue: Int
  protected abstract val maxMinorValue: Int
  protected abstract val maxBuildValue: Int

  fun versionAsLong(vararg components: Int): Long {
    val baselineVersion = components.getOrElse(0) { 0 }
    val build = components.getOrElse(1) { 0 }
    var longVersion = branchBuildAsLong(baselineVersion, build)

    if (components.size >= 3) {
      val component = components[2]
      longVersion += if (component >= maxMinorValue) maxMinorValue - 1 else component
    }

    return longVersion
  }

  fun getMaxVersionAsLong(): Long {
    return branchBuildAsLong(maxBranchValue - 1, maxBuildValue - 1)
  }

  private fun branchBuildAsLong(branch: Int, build: Int): Long {
    val result = if (build >= maxBuildValue) {
      maxBuildValue - 1
    } else {
      build
    }

    return branch.toLong() * maxMinorValue * maxBuildValue + result.toLong() * maxMinorValue
  }
}

object ReSharperCompatibilityUtils: BaseCompatibilityUtils() {
  override val maxBranchValue: Int
    get() = 10000
  override val maxMinorValue: Int
    get() = 10
  override val maxBuildValue: Int
    get() = 10

  fun getMaxBuild() = maxBuildValue - 1
  fun getMaxMinor() = maxMinorValue - 1
}

object CompatibilityUtils: BaseCompatibilityUtils() {
  override val maxBranchValue: Int
    get() = 1000
  override val maxMinorValue: Int
    get() = 10000
  override val maxBuildValue: Int
    get() = 100000
}
