package com.jetbrains.plugin.structure.classes.resolvers

import org.objectweb.asm.tree.ClassNode

object EmptyResolver : Resolver() {
  override val readMode
    get() = ReadMode.FULL

  override fun processAllClasses(processor: (ClassNode) -> Boolean) = true

  override fun resolveClass(className: String) = ResolutionResult.NotFound

  override fun containsClass(className: String) = false

  override fun containsPackage(packageName: String) = false

  override val allClasses = emptySet<String>()

  override val allPackages = emptySet<String>()

  override val isEmpty = true

  override fun toString() = "EmptyResolver"

  override fun close() = Unit

}
