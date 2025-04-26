package com.jetbrains.plugin.structure.ide.classes.resolver

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import java.util.*

fun Collection<Resolver>.unique(): Collection<Resolver> {
  return when (this.size) {
    0 -> emptySet<Resolver>()
    1 -> this
    else -> {
      val result = IdentityHashMap<Resolver, Boolean>()
      for (resolver in this) {
        result[resolver] = true
      }
      return result.keys
    }
  }
}
