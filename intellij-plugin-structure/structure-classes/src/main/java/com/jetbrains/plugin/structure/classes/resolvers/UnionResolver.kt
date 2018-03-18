package com.jetbrains.plugin.structure.classes.resolvers

import org.objectweb.asm.tree.ClassNode
import java.io.IOException

/**
 * [Resolver] that unites several [resolvers] with the class-path like order
 * of the classes [resolution] [findClass].
 */
class UnionResolver private constructor(private val resolvers: List<Resolver>) : Resolver() {

  private val classNameToResolver: Map<String, Resolver> = {
    val result = hashMapOf<String, Resolver>()
    /**
     * We want to support the class path search order,
     * so the `class-name -> resolver` mapping should prefer
     * the first resolver in the [resolvers] list in
     * case several resolvers contain the same class.
     */
    for (resolver in resolvers.asReversed()) {
      for (className in resolver.allClasses) {
        result[className] = resolver
      }
    }
    result
  }()

  override val allClasses
    get() = classNameToResolver.keys

  override val isEmpty
    get() = classNameToResolver.isEmpty()

  override val classPath
    get() = resolvers.flatMap { it.classPath }

  override val finalResolvers
    get() = resolvers.flatMap { it.finalResolvers }

  override fun processAllClasses(processor: (ClassNode) -> Boolean) =
      resolvers
          .asSequence()
          .all { it.processAllClasses(processor) }

  override fun containsClass(className: String) = className in classNameToResolver

  @Throws(IOException::class)
  override fun findClass(className: String) = classNameToResolver[className]?.findClass(className)

  override fun getClassLocation(className: String) = classNameToResolver[className]?.getClassLocation(className)

  @Throws(IOException::class)
  override fun close() {
    val firstException = resolvers.asSequence()
        .mapNotNull {
          try {
            it.close()
            null
          } catch (e: Exception) {
            e
          }
        }.firstOrNull()

    if (firstException != null) {
      throw firstException
    }
  }

  override fun toString() = "Union of ${resolvers.size} resolver" + (if (resolvers.size != 1) "s" else "")

  companion object {

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
