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
  cacheSize: Int = getDefaultCacheSize()
) : Resolver() {

  private data class BundleCacheKey(val baseName: String, val locale: Locale)

  private val classCache: LoadingCache<BinaryClassName, ResolutionResult<ClassNode>> =
    Caffeine.newBuilder()
      .softValues()
      .maximumSize(cacheSize.toLong())
      .build { key -> delegate.resolveClass(key) }

  private val propertyBundleCache: LoadingCache<BundleCacheKey, ResolutionResult<PropertyResourceBundle>> =
    Caffeine.newBuilder()
      .softValues()
      .maximumSize(cacheSize.toLong())
      .build { key -> delegate.resolveExactPropertyResourceBundle(key.baseName, key.locale) }

  @Deprecated("Use 'allClassNames' property instead which is more efficient")
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

  @Deprecated("Use 'resolveClass(BinaryClassName)' instead")
  override fun resolveClass(className: String): ResolutionResult<ClassNode> = resolveClass(className as BinaryClassName)

  override fun resolveClass(className: BinaryClassName): ResolutionResult<ClassNode> = try {
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

  @Deprecated("Use 'containsClass(BinaryClassName)' instead")
  override fun containsClass(className: String) =
    delegate.containsClass(className)

  override fun containsClass(className: BinaryClassName) = delegate.containsClass(className)

  override fun containsPackage(packageName: String) =
    delegate.containsPackage(packageName)

  override fun close() {
    delegate.close()
  }

  override fun processAllClasses(processor: (ResolutionResult<ClassNode>) -> Boolean) =
    delegate.processAllClasses(processor)

  private companion object {
    fun getDefaultCacheSize(): Int =
      System.getProperty("intellij.structure.classes.cache.size", "1024").toIntOrNull() ?: 1024
  }
}
