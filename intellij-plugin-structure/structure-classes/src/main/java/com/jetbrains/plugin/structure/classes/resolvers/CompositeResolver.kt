/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.base.BinaryClassName
import com.jetbrains.plugin.structure.base.utils.binaryClassNames
import com.jetbrains.plugin.structure.base.utils.closeAll
import org.objectweb.asm.tree.ClassNode
import java.util.*


private const val DEFAULT_COMPOSITE_RESOLVER_NAME = "Unnamed Composite Resolver"
/**
 * [Resolver] that combines several [resolvers] with the Java classpath search strategy.
 */
class CompositeResolver private constructor(
  private val resolvers: List<Resolver>,
  override val readMode: ReadMode,
  val name: String
) : Resolver() {

  private val packageToResolvers: MutableMap<String, MutableList<Resolver>> = hashMapOf()

  private val fullBundleNames = hashMapOf<String, MutableSet<String>>()

  private val baseBundleNameToResolvers: MutableMap<String, MutableList<Resolver>> = hashMapOf()

  init {
    buildIndex()
  }

  private fun buildIndex() {
    for (resolver in resolvers) {
      for (packageName in resolver.allPackages) {
        packageToResolvers.getOrPut(packageName) { arrayListOf() } += resolver
      }

      val bundleNameSet = resolver.allBundleNameSet
      for (baseBundleName in bundleNameSet.baseBundleNames) {
        baseBundleNameToResolvers.getOrPut(baseBundleName) { arrayListOf() } += resolver

        val resolverAllNames = bundleNameSet[baseBundleName]
        if (resolverAllNames.isNotEmpty()) {
          fullBundleNames.getOrPut(baseBundleName) { hashSetOf() } += resolverAllNames
        }
      }
    }
  }

  override val allClasses
    get() = resolvers.flatMapTo(hashSetOf()) { it.allClasses }

  override val allClassNames: Set<BinaryClassName>
    get() = resolvers.flatMapTo(binaryClassNames()) { it.allClassNames }

  override val allBundleNameSet: ResourceBundleNameSet
    get() = ResourceBundleNameSet(fullBundleNames)

  @Deprecated("Use 'packages' property instead. This property may be slow on some file systems.")
  override val allPackages
    get() = packageToResolvers.keys

  override val packages: Set<String>
    get() = resolvers.flatMapTo(hashSetOf()) { it.packages }

  override fun processAllClasses(processor: (ResolutionResult<ClassNode>) -> Boolean) =
    resolvers.all { it.processAllClasses(processor) }

  private fun getPackageName(className: String) = className.substringBeforeLast('/', "")

  override fun containsClass(className: String): Boolean {
    val packageName = getPackageName(className)
    val resolvers = packageToResolvers[packageName] ?: emptyList()
    return resolvers.any { it.containsClass(className) }
  }

  override fun containsPackage(packageName: String) = packageName in packageToResolvers

  override fun resolveClass(className: String): ResolutionResult<ClassNode> {
    val packageName = getPackageName(className)
    val resolvers = packageToResolvers[packageName] ?: emptyList()
    for (resolver in resolvers) {
      val resolutionResult = resolver.resolveClass(className)
      if (resolutionResult !is ResolutionResult.NotFound) {
        return resolutionResult
      }
    }
    return ResolutionResult.NotFound
  }

  override fun resolveExactPropertyResourceBundle(baseName: String, locale: Locale): ResolutionResult<PropertyResourceBundle> {
    val resolvers = baseBundleNameToResolvers[baseName] ?: emptyList()
    for (resolver in resolvers) {
      val resolutionResult = resolver.resolveExactPropertyResourceBundle(baseName, locale)
      if (resolutionResult !is ResolutionResult.NotFound) {
        return resolutionResult
      }
    }
    return ResolutionResult.NotFound
  }

  override fun close() {
    resolvers.closeAll()
  }

  override fun toString() = "$name is a union of ${resolvers.size} resolver" + (if (resolvers.size != 1) "s" else "")

  companion object {

    @JvmStatic
    fun create(vararg resolvers: Resolver): Resolver = create(resolvers.asIterable())

    @JvmStatic
    fun create(resolvers: Iterable<Resolver>): Resolver {
      return create(resolvers, DEFAULT_COMPOSITE_RESOLVER_NAME)
    }

    @JvmStatic
    fun create(resolvers: Iterable<Resolver>, resolverName: String): Resolver {
      val list = resolvers.toList()
      return when(list.size) {
        0 -> EmptyResolver(resolverName)
        1 -> list.first()
        else -> {
          val readMode = if (list.all { it.readMode == ReadMode.FULL }) {
            ReadMode.FULL
          } else {
            ReadMode.SIGNATURES
          }
          SimpleCompositeResolver(list, readMode, resolverName)
        }
      }
    }
  }
}
