package com.jetbrains.pluginverifier.utils

import java.util.Comparator
import java.util.IdentityHashMap

open class ToStringCachedComparator<T> : Comparator<T> {

  private val myCache = IdentityHashMap<T, String>()

  protected open fun toString(`object`: T): String {
    return `object`.toString()
  }

  private fun getDescriptor(obj: T): String {
    var res: String? = myCache[obj]
    if (res == null) {
      res = toString(obj)
      myCache.put(obj, res)
    }

    return res
  }

  override fun compare(o1: T, o2: T): Int {
    return getDescriptor(o1).compareTo(getDescriptor(o2))
  }
}
