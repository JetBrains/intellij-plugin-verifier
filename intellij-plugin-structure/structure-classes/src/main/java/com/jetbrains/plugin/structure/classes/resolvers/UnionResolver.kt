package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.base.utils.closeAll
import org.objectweb.asm.tree.ClassNode
import java.nio.file.Path

/**
 * [Resolver] that unites several [resolvers] with the Java classpath search strategy.
 */
class UnionResolver private constructor(
    private val resolvers: List<Resolver>,
    override val readMode: ReadMode
) : Resolver() {

  private val packageToResolvers: Map<String, List<Resolver>> = buildPackageToResolvers()

  private fun buildPackageToResolvers(): Map<String, List<Resolver>> {
    val result = hashMapOf<String, MutableList<Resolver>>()
    for (resolver in resolvers) {
      for (packageName in resolver.allPackages) {
        result.getOrPut(packageName) { arrayListOf() } += resolver
      }
    }
    return result
  }

  override val allClasses
    get() = resolvers.flatMapTo(hashSetOf()) { it.allClasses }

  override val allPackages
    get() = packageToResolvers.keys

  override val isEmpty
    get() = packageToResolvers.isEmpty()

  override val classPath: List<Path>
    get() = resolvers.flatMap { it.classPath }

  override val finalResolvers
    get() = resolvers.flatMap { it.finalResolvers }

  override fun processAllClasses(processor: (ClassNode) -> Boolean) =
      resolvers.asSequence().all { it.processAllClasses(processor) }

  private fun getPackageName(className: String) = className.substringBeforeLast('/', "")

  override fun containsClass(className: String): Boolean {
    val packageName = getPackageName(className)
    val resolvers = packageToResolvers[packageName]
    return resolvers != null && resolvers.any { it.containsClass(className) }
  }

  override fun containsPackage(packageName: String) = packageName in packageToResolvers

  override fun findClass(className: String): ClassNode? {
    val packageName = getPackageName(className)
    val resolvers = packageToResolvers[packageName]
    if (resolvers == null || resolvers.isEmpty()) {
      return null
    }
    for (resolver in resolvers) {
      val classNode = resolver.findClass(className)
      if (classNode != null) {
        return classNode
      }
    }
    return null
  }

  override fun getClassLocation(className: String): Resolver? {
    val packageName = getPackageName(className)
    val resolvers = packageToResolvers[packageName]
    if (resolvers == null || resolvers.isEmpty()) {
      return null
    }
    for (resolver in resolvers) {
      val classLocation = resolver.getClassLocation(className)
      if (classLocation != null) {
        return classLocation
      }
    }
    return null
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
      val nonEmpty = resolvers.filterNot { it.isEmpty }
      return when {
        nonEmpty.isEmpty() -> EmptyResolver
        nonEmpty.size == 1 -> nonEmpty[0]
        else -> {
          /**
           * Remove duplicate Resolvers built
           * from the same class paths.
           */
          val uniqueResolvers = nonEmpty
              .flatMap { it.finalResolvers }
              .distinctBy { it.classPath }

          val readMode = if (uniqueResolvers.all { it.readMode == ReadMode.FULL }) {
            ReadMode.FULL
          } else {
            ReadMode.SIGNATURES
          }

          UnionResolver(uniqueResolvers, readMode)
        }
      }
    }
  }
}
