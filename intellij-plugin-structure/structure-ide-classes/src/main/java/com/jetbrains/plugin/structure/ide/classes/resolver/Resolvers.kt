/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.classes.resolver

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import java.util.*

fun <R : Resolver> Collection<R>.unique(): Collection<R> {
  return when (this.size) {
    0 -> emptySet()
    1 -> this
    else -> {
      val result = IdentityHashMap<R, Boolean>()
      for (resolver in this) {
        result[resolver] = true
      }
      result.keys
    }
  }
}

fun <R : Resolver> List<R>.unique(): List<R> {
  return when (this.size) {
    0 -> emptyList()
    1 -> this
    else -> {
      val result = ArrayList<R>(this.size)
      val map = IdentityHashMap<R, Boolean>()
      for (resolver in this) {
        if (map.put(resolver, true) == null) {
          result.add(resolver)
        }
      }
      result.trimToSize()
      result
    }
  }
}
