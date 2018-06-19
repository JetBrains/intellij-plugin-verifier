package com.jetbrains.plugin.structure.classes.resolvers

import org.objectweb.asm.tree.ClassNode
import java.io.File

object EmptyResolver : Resolver() {
  override fun processAllClasses(processor: (ClassNode) -> Boolean) = true

  override fun findClass(className: String) = null

  override fun getClassLocation(className: String) = null

  override fun containsClass(className: String) = false

  override fun containsPackage(packageName: String) = false

  override val allClasses = emptySet<String>()

  override val allPackages = emptySet<String>()

  override val isEmpty = true

  override val classPath = emptyList<File>()

  override val finalResolvers = emptyList<Resolver>()

  override fun toString() = "EmptyResolver"

  override fun close() = Unit

}
