package com.jetbrains.pluginverifier.utils

import com.jetbrains.pluginverifier.problems.Problem

class ToStringProblemComparator : ToStringCachedComparator<Problem>() {
  override fun toString(`object`: Problem): String {
    return `object`.description
  }
}
