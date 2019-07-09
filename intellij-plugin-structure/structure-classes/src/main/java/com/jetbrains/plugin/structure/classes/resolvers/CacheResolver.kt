package com.jetbrains.plugin.structure.classes.resolvers

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import org.objectweb.asm.tree.ClassNode
import java.util.concurrent.ExecutionException

class CacheResolver
@JvmOverloads constructor(
    private val delegate: Resolver,
    cacheSize: Int = DEFAULT_CACHE_SIZE
) : Resolver() {

  private val cache: LoadingCache<String, ResolutionResult> =
      CacheBuilder.newBuilder()
          .maximumSize(cacheSize.toLong())
          .build(object : CacheLoader<String, ResolutionResult>() {
            override fun load(key: String) = delegate.resolveClass(key)
          })

  override val allClasses
    get() = delegate.allClasses

  override val allPackages
    get() = delegate.allPackages

  override val isEmpty
    get() = delegate.isEmpty

  override val readMode
    get() = delegate.readMode

  override fun resolveClass(className: String): ResolutionResult = try {
    cache.get(className)
  } catch (e: ExecutionException) {
    throw e.cause ?: e
  }

  override fun toString() = "Caching resolver for $delegate"

  override fun containsClass(className: String) =
      delegate.containsClass(className)

  override fun containsPackage(packageName: String) =
      delegate.containsPackage(packageName)

  override fun close() {
    delegate.close()
  }

  override fun processAllClasses(processor: Function1<ClassNode, Boolean>) =
      delegate.processAllClasses(processor)

  private companion object {
    private const val DEFAULT_CACHE_SIZE = 1024
  }
}
