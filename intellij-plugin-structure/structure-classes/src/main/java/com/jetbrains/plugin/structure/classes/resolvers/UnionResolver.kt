package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.base.utils.closeAll
import com.jetbrains.plugin.structure.classes.packages.PackageSet
import org.objectweb.asm.tree.ClassNode
import java.io.IOException

/**
 * [Resolver] that unites several [resolvers] with the class-path like order
 * of the classes [resolution] [findClass].
 */
class UnionResolver private constructor(private val resolvers: List<Resolver>) : Resolver() {

  private val classToResolver = hashMapOf<String, Resolver>()

  private val packageSet = PackageSet()

  init {
    /**
     * We want to support the class path search order,
     * so the `class-name -> resolver` mapping should prefer
     * the first resolver in the [resolvers] list in
     * case several resolvers contain the same class.
     */
    for (resolver in resolvers.asReversed()) {
      for (className in resolver.allClasses) {
        classToResolver[className] = resolver
      }
      packageSet.addPackages(resolver.allPackages)
    }
  }

  override val allClasses
    get() = classToResolver.keys

  override val allPackages
    get() = packageSet.getAllPackages()

  override val isEmpty
    get() = classToResolver.isEmpty()

  override val classPath
    get() = resolvers.flatMap { it.classPath }

  override val finalResolvers
    get() = resolvers.flatMap { it.finalResolvers }

  override fun processAllClasses(processor: (ClassNode) -> Boolean) =
      resolvers
          .asSequence()
          .all { it.processAllClasses(processor) }

  override fun containsClass(className: String) = className in classToResolver

  override fun containsPackage(packageName: String) = packageSet.containsPackage(packageName)

  @Throws(IOException::class)
  override fun findClass(className: String) = classToResolver[className]?.findClass(className)

  override fun getClassLocation(className: String) = classToResolver[className]?.getClassLocation(className)

  @Throws(IOException::class)
  override fun close() {
    resolvers.closeAll()
  }

  override fun toString() = "Union of ${resolvers.size} resolver" + (if (resolvers.size != 1) "s" else "")

  companion object {

    @JvmStatic
    fun create(vararg resolvers: Resolver): Resolver {
      return create(resolvers.asIterable())
    }

    @JvmStatic
    fun create(resolvers: Iterable<Resolver>): Resolver {
      val nonEmpty = resolvers.filterNot { it.isEmpty }
      return when {
        nonEmpty.isEmpty() -> EmptyResolver
        nonEmpty.size == 1 -> nonEmpty[0]
        else -> {
          /**
           * Remove duplicate Resolvers built
           * from the same class paths.
           */
          /**
           * Remove duplicate Resolvers built
           * from the same class paths.
           */
          val uniqueResolvers = nonEmpty
              .flatMap { it.finalResolvers }
              .distinctBy { it.classPath }
          UnionResolver(uniqueResolvers)
        }
      }
    }
  }
}
