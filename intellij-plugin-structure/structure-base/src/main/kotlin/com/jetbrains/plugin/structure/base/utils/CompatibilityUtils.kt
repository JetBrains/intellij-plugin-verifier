package com.jetbrains.plugin.structure.base.utils

import java.util.ArrayList


class CompatibilityUtils {
  companion object {
    const val MAX_BRANCH_VALUE = 1000
    const val MAX_BUILD_VALUE = 100000
    private const val MAX_COMPONENT_VALUE = 10000
    private val NUMBERS_OF_NINES by lazy { initNumberOfNines() }

    fun versionAsLong(vararg components: Int): Long {
      val baselineVersion = components.getOrElse(0) { 0 }
      val build = components.getOrElse(1) { 0 }
      var longVersion = branchBuildAsLong(baselineVersion, build)

      if (components.size >= 3) {
        val component = components[2]
        longVersion += if (component == Integer.MAX_VALUE) MAX_COMPONENT_VALUE - 1 else component
      }

      return longVersion
    }

    @Deprecated(
      message = "Use IdeVersion.maxVersion.asLong()",
      replaceWith = ReplaceWith("IdeVersion.maxVersion.asLong()")
    )
    fun getMaxVersionAsLong(): Long {
      return branchBuildAsLong(MAX_BRANCH_VALUE - 1, MAX_BUILD_VALUE - 1)
    }

    private fun branchBuildAsLong(branch: Int, build: Int): Long {
      val result = if (build == Integer.MAX_VALUE || isNumberOfNines(build)) {
        MAX_BUILD_VALUE - 1
      } else {
        build
      }

      return branch.toLong() * MAX_COMPONENT_VALUE * MAX_BUILD_VALUE + result.toLong() * MAX_COMPONENT_VALUE
    }

    private fun isNumberOfNines(p: Int) = NUMBERS_OF_NINES.any { it == p }

    private fun initNumberOfNines(): IntArray {
      val numbersOfNines = ArrayList<Int>()
      var i = 99999
      val maxIntDiv10 = Integer.MAX_VALUE / 10
      while (i < maxIntDiv10) {
        i = i * 10 + 9
        numbersOfNines.add(i)
      }

      return numbersOfNines.toIntArray()
    }
  }
}
