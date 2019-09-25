package com.jetbrains.plugin.structure.classes.resolvers

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import org.objectweb.asm.tree.ClassNode
import java.util.*
import java.util.concurrent.ExecutionException

class CacheResolver(
    private val delegate: Resolver,
    cacheSize: Int = DEFAULT_CACHE_SIZE
) : Resolver() {

  private data class BundleCacheKey(val baseName: String, val locale: Locale)

  private val classCache: LoadingCache<String, ResolutionResult<ClassNode>> =
      CacheBuilder.newBuilder()
          .maximumSize(cacheSize.toLong())
          .build(object : CacheLoader<String, ResolutionResult<ClassNode>>() {
            override fun load(key: String) = delegate.resolveClass(key)
          })

  private val propertyBundleCache: LoadingCache<BundleCacheKey, ResolutionResult<PropertyResourceBundle>> =
      CacheBuilder.newBuilder()
          .maximumSize(cacheSize.toLong())
          .build(object : CacheLoader<BundleCacheKey, ResolutionResult<PropertyResourceBundle>>() {
            override fun load(key: BundleCacheKey) = delegate.resolveExactPropertyResourceBundle(key.baseName, key.locale)
          })

  override val allClasses
    get() = delegate.allClasses

  override val allBundleNameSet
    get() = delegate.allBundleNameSet

  override val allPackages
    get() = delegate.allPackages

  override val readMode
    get() = delegate.readMode

  override fun resolveClass(className: String): ResolutionResult<ClassNode> = try {
    classCache.get(className)
  } catch (e: ExecutionException) {
    throw e.cause ?: e
  }

  override fun resolveExactPropertyResourceBundle(baseName: String, locale: Locale): ResolutionResult<PropertyResourceBundle> = try {
    propertyBundleCache.get(BundleCacheKey(baseName, locale))
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
