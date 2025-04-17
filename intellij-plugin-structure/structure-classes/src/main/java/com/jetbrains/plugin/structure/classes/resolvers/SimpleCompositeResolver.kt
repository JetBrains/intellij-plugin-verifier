package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.base.utils.closeAll
import org.objectweb.asm.tree.ClassNode
import java.util.*

class SimpleCompositeResolver internal constructor(
  private val resolvers: List<Resolver>,
  override val readMode: ReadMode,
  val name: String
) : Resolver() {

  override val allClasses: Set<String>
    get() = resolvers.flatMapTo(hashSetOf()) { it.allClasses }

  @Deprecated("Use 'packages' property instead. This property may be slow on some file systems.")
  override val allPackages: Set<String>
    get() = resolvers.flatMapTo(hashSetOf()) { it.allPackages }

  override val packages: Set<String>
    get() = resolvers.flatMapTo(hashSetOf()) { it.packages }

  override val allBundleNameSet: ResourceBundleNameSet
    get() = resolvers.map {
      it.allBundleNameSet
    }.reduce { acc, bundleNames -> acc.merge(bundleNames) }

  override fun resolveClass(className: String): ResolutionResult<ClassNode> {
    for (resolver in resolvers) {
      val resolutionResult = resolver.resolveClass(className)
      if (resolutionResult !is ResolutionResult.NotFound) {
        return resolutionResult
      }
    }
    return ResolutionResult.NotFound
  }

  override fun resolveExactPropertyResourceBundle(
    baseName: String,
    locale: Locale
  ): ResolutionResult<PropertyResourceBundle> {
    for (resolver in resolvers) {
      val resolutionResult = resolver.resolveExactPropertyResourceBundle(baseName, locale)
      if (resolutionResult !is ResolutionResult.NotFound) {
        return resolutionResult
      }
    }
    return ResolutionResult.NotFound
  }

  override fun containsClass(className: String): Boolean {
    return resolvers.any { it.containsClass(className) }
  }

  override fun containsPackage(packageName: String): Boolean {
    return resolvers.any { it.containsPackage(packageName) }
  }

  override fun processAllClasses(processor: (ResolutionResult<ClassNode>) -> Boolean): Boolean {
    return resolvers.all { it.processAllClasses(processor) }
  }

  override fun close() {
    resolvers.closeAll()
  }

  override fun toString(): String {
    return "$name is a composite of ${resolvers.size} resolver" + (if (resolvers.size != 1) "s" else "")
  }

  private fun ResourceBundleNameSet.merge(resourceBundleNameSet: ResourceBundleNameSet): ResourceBundleNameSet {
    val mergedBundles = HashMap<String, Set<String>>(bundleNames)
    resourceBundleNameSet.bundleNames.forEach { (baseName, localeSpecificNames) ->
      mergedBundles.merge(baseName, localeSpecificNames) { existing, new -> existing + new }
    }
    return ResourceBundleNameSet(mergedBundles)
  }

  companion object {
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