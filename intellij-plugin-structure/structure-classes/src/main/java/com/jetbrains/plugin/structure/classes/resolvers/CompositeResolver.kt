/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.base.utils.closeAll
import org.objectweb.asm.tree.ClassNode
import java.util.*

/**
 * [Resolver] that combines several [resolvers] with the Java classpath search strategy.
 */
class CompositeResolver private constructor(
  private val resolvers: List<Resolver>,
  override val readMode: ReadMode
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

  override val allBundleNameSet: ResourceBundleNameSet
    get() = ResourceBundleNameSet(fullBundleNames)

  override val allPackages
    get() = packageToResolvers.keys

  override fun processAllClasses(processor: (ResolutionResult<ClassNode>) -> Boolean) =
    resolvers.asSequence().all { it.processAllClasses(processor) }

  private fun getPackageName(className: String) = className.substringBeforeLast('/', "")

  override fun containsClass(className: String): Boolean {
    val packageName = getPackageName(className)
    val resolvers = packageToResolvers[packageName]
    return resolvers != null && resolvers.any { it.containsClass(className) }
  }

  override fun containsPackage(packageName: String) = packageName in packageToResolvers

  override fun resolveClass(className: String): ResolutionResult<ClassNode> {
    val packageName = getPackageName(className)
    val resolvers = packageToResolvers[packageName]
    if (resolvers == null || resolvers.isEmpty()) {
      return ResolutionResult.NotFound
    }
    for (resolver in resolvers) {
      val resolutionResult = resolver.resolveClass(className)
      if (resolutionResult !is ResolutionResult.NotFound) {
        return resolutionResult
      }
    }
    return ResolutionResult.NotFound
  }

  override fun resolveExactPropertyResourceBundle(baseName: String, locale: Locale): ResolutionResult<PropertyResourceBundle> {
    val resolvers = baseBundleNameToResolvers[baseName]
    if (resolvers == null || resolvers.isEmpty()) {
      return ResolutionResult.NotFound
    }
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

  override fun toString() = "Union of ${resolvers.size} resolver" + (if (resolvers.size != 1) "s" else "")

  companion object {

    @JvmStatic
    fun create(vararg resolvers: Resolver): Resolver = create(resolvers.asIterable())

    @JvmStatic
    fun create(resolvers: Iterable<Resolver>): Resolver {
      val list = resolvers.toList()
      if (list.isEmpty()) {
        return EmptyResolver
      }
      if (list.size == 1) {
        return list.first()
      }
      val readMode = if (list.all { it.readMode == ReadMode.FULL }) {
        ReadMode.FULL
      } else {
        ReadMode.SIGNATURES
      }
      return CompositeResolver(list, readMode)
    }
  }
}
