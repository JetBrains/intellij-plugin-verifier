package com.jetbrains.plugin.structure.classes.resolvers

import com.google.common.collect.Iterators
import org.objectweb.asm.tree.ClassNode

import java.io.File
import java.io.IOException

class UnionResolver private constructor(private val resolvers: List<Resolver>) : Resolver() {

  override fun getAllClasses(): Iterator<String> = Iterators.concat(resolvers.map { it.allClasses }.iterator())

  override fun isEmpty(): Boolean = resolvers.all { it.isEmpty }

  override fun containsClass(className: String): Boolean = resolvers.any { it.containsClass(className) }

  override fun getClassPath(): List<File> = resolvers.flatMap { it.classPath }

  override fun getFinalResolvers(): List<Resolver> = resolvers.flatMap { it.finalResolvers }

  @Throws(IOException::class)
  override fun findClass(className: String): ClassNode? = resolvers
      .asSequence()
      .map { it.findClass(className) }
      .firstOrNull { it != null }

  override fun getClassLocation(className: String): Resolver? = resolvers
      .asSequence()
      .map { it.getClassLocation(className) }
      .firstOrNull { it != null }

  @Throws(IOException::class)
  override fun close() {
    var first: IOException? = null
    for (resolver in resolvers) {
      try {
        resolver.close()
      } catch (e: IOException) {
        first = e
      }
    }

    if (first != null) {
      throw first
    }
  }

  companion object {

    @JvmStatic
    fun create(resolvers: Iterable<Resolver>): Resolver {
      val nonEmptyResolvers = resolvers.filterNot { it.isEmpty }
      if (nonEmptyResolvers.isEmpty()) {
        return EmptyResolver
      } else if (nonEmptyResolvers.size == 1) {
        return nonEmptyResolvers[0]
      } else {
        val finalResolvers = nonEmptyResolvers.flatMap { it.finalResolvers }
        val uniqueResolvers = finalResolvers.distinctBy { it.classPath }
        return UnionResolver(uniqueResolvers)
      }
    }
  }
}
