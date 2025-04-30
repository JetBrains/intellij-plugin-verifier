/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.classes.resolvers

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.jetbrains.plugin.structure.base.BinaryClassName
import org.objectweb.asm.tree.ClassNode
import java.util.*
import java.util.concurrent.ExecutionException

class CacheResolver(
  private val delegate: Resolver,
  cacheSize: Int = DEFAULT_CACHE_SIZE
) : Resolver() {

  private data class BundleCacheKey(val baseName: String, val locale: Locale)

  private val classCache: LoadingCache<String, ResolutionResult<ClassNode>> =
    Caffeine.newBuilder()
      .maximumSize(cacheSize.toLong())
      .build { key -> delegate.resolveClass(key) }

  private val propertyBundleCache: LoadingCache<BundleCacheKey, ResolutionResult<PropertyResourceBundle>> =
    Caffeine.newBuilder()
      .maximumSize(cacheSize.toLong())
      .build { key -> delegate.resolveExactPropertyResourceBundle(key.baseName, key.locale) }

  override val allClasses
    get() = delegate.allClasses

  override val allClassNames: Set<BinaryClassName>
    get() = delegate.allClassNames

  override val allBundleNameSet
    get() = delegate.allBundleNameSet

  @Deprecated("Use 'packages' property instead. This property may be slow on some file systems.")
  override val allPackages
    get() = delegate.allPackages

  override val packages: Set<String>
    get() = delegate.packages

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

  override fun processAllClasses(processor: (ResolutionResult<ClassNode>) -> Boolean) =
    delegate.processAllClasses(processor)

  private companion object {
    private const val DEFAULT_CACHE_SIZE = 1024
  }
}
