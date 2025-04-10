package com.jetbrains.plugin.structure.classes.resolvers

import org.objectweb.asm.tree.ClassNode
import java.util.*

class LazyCompositeResolver private constructor(
  resolvers: List<Resolver>,
  override val readMode: ReadMode,
  name: String
) : NamedResolver(name) {

  private val delegateResolver by lazy {
    CompositeResolver.create(resolvers, name)
  }

  override val allClasses: Set<String>
    get() = delegateResolver.allClasses

  @Deprecated("Use 'packages' property instead. This property may be slow on some file systems.")
  override val allPackages: Set<String>
    get() = delegateResolver.allPackages

  override val packages: Set<String>
    get() = delegateResolver.packages

  override val allBundleNameSet: ResourceBundleNameSet
    get() = delegateResolver.allBundleNameSet

  override fun resolveClass(className: String): ResolutionResult<ClassNode> = delegateResolver.resolveClass(className)

  override fun resolveExactPropertyResourceBundle(
    baseName: String,
    locale: Locale
  ): ResolutionResult<PropertyResourceBundle> = delegateResolver.resolveExactPropertyResourceBundle(baseName, locale)

  override fun containsClass(className: String): Boolean = delegateResolver.containsClass(className)

  override fun containsPackage(packageName: String): Boolean = delegateResolver.containsPackage(packageName)

  override fun processAllClasses(processor: (ResolutionResult<ClassNode>) -> Boolean): Boolean =
    delegateResolver.processAllClasses(processor)

  override fun close(): Unit = delegateResolver.close()

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
          LazyCompositeResolver(list, readMode, resolverName)
        }
      }
    }

    @JvmStatic
    fun create(resolvers: Iterable<NamedResolver>, resolverName: String): NamedResolver {
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
          LazyCompositeResolver(list, readMode, resolverName)
        }
      }
    }
  }
}