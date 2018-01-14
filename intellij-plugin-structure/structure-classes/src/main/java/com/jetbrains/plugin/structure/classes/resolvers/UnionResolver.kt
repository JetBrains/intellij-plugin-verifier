package com.jetbrains.plugin.structure.classes.resolvers

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

  override fun getAllClasses() = classNameToResolver.keys

  override fun isEmpty() = classNameToResolver.isEmpty()

  override fun containsClass(className: String) = className in classNameToResolver

  override fun getClassPath() = resolvers.flatMap { it.classPath }

  override fun getFinalResolvers() = resolvers.flatMap { it.finalResolvers }

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
