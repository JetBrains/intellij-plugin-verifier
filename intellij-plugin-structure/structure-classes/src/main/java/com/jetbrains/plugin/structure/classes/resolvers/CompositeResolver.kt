package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.base.utils.closeAll
import org.objectweb.asm.tree.ClassNode

/**
 * [Resolver] that combines several [resolvers] with the Java classpath search strategy.
 */
class CompositeResolver private constructor(
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

  override fun processAllClasses(processor: (ClassNode) -> Boolean) =
      resolvers.asSequence().all { it.processAllClasses(processor) }

  private fun getPackageName(className: String) = className.substringBeforeLast('/', "")

  override fun containsClass(className: String): Boolean {
    val packageName = getPackageName(className)
    val resolvers = packageToResolvers[packageName]
    return resolvers != null && resolvers.any { it.containsClass(className) }
  }

  override fun containsPackage(packageName: String) = packageName in packageToResolvers

  override fun resolveClass(className: String): ResolutionResult {
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
          val readMode = if (nonEmpty.all { it.readMode == ReadMode.FULL }) {
            ReadMode.FULL
          } else {
            ReadMode.SIGNATURES
          }

          CompositeResolver(nonEmpty, readMode)
        }
      }
    }
  }
}
