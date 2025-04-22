package com.jetbrains.plugin.structure.ide.classes.resolver

import com.jetbrains.plugin.structure.classes.resolvers.CacheResolver
import com.jetbrains.plugin.structure.classes.resolvers.CompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.LazyCompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.NamedResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.SimpleCompositeResolver
import java.util.*
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

fun Resolver.flatten(): Collection<Resolver> {
  return when (this) {
    is CacheResolver -> accessDelegate(this, "delegate")?.flatten()?: emptyList<Resolver>()
    is CompositeResolver -> accessResolvers(this, "resolvers").flatMap { it.flatten() }
    is SimpleCompositeResolver -> accessResolvers(this, "resolvers").flatMap { it.flatten() }
    is LazyCompositeResolver -> accessDelegate(this, "delegateResolver")?.flatten() ?: emptyList<Resolver>()
    is ProductInfoClassResolver -> namedResolvers.flatMap { it.flatten() }
    is CachingPluginDependencyResolverProvider.ComponentNameAwareCompositeResolver -> accessDelegate(this,
      "delegateResolver")?.flatten() ?: emptyList()
    else -> listOf(this)
  }.uniqueWithStats().let {
    if (it.second.isNotEmpty()) {
      val dups = it.second.filterValues { it > 1 }.keys
      println("Duplicates: ${dups}")
    }
    it.first
  }
}

fun Collection<Resolver>.uniqueWithStats(): Pair<Collection<Resolver>, Map<String, Int>> {
  val stats = mutableMapOf<String, Int>()
  return when (this.size) {
    0 -> emptySet<Resolver>() to stats
    1 -> this to stats
    else -> {
      val result = IdentityHashMap<Resolver, Boolean>()
      for (resolver in this) {
        if (result.containsKey(resolver)) {
          val resolverName = if (resolver is NamedResolver) resolver.name else resolver.javaClass.name
          stats[resolverName] = stats.getOrDefault(resolverName, 0) + 1
        }
        result[resolver] = true
      }
      result.keys to stats
    }
  }
}

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

fun accessDelegate(resolver: Resolver, propertyName: String): Resolver? {
  val kClass = resolver::class
  val delegateProperty = kClass
    .declaredMemberProperties
    .firstOrNull { it.name == propertyName }

  return delegateProperty?.run {
    isAccessible = true
    getter.call(resolver) as? Resolver
  }
}

private fun accessResolvers(resolver: Resolver, propertyName: String): List<Resolver> {
  val kClass = resolver::class
  val delegateProperty = kClass
    .declaredMemberProperties
    .firstOrNull { it.name == propertyName }

  val nestedResolvers = delegateProperty?.run {
    isAccessible = true
    getter.call(resolver) as? List<Resolver>
  } ?: emptyList()
  println("resolvers for $kClass: $nestedResolvers")
  return nestedResolvers
}
