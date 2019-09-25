package com.jetbrains.plugin.structure.classes.resolvers

import org.objectweb.asm.tree.ClassNode
import java.util.*

object EmptyResolver : Resolver() {
  override val readMode
    get() = ReadMode.FULL

  override fun processAllClasses(processor: (ClassNode) -> Boolean) = true

  override fun resolveClass(className: String) = ResolutionResult.NotFound

  override fun resolveExactPropertyResourceBundle(baseName: String, locale: Locale) = ResolutionResult.NotFound

  override fun containsClass(className: String) = false

  override fun containsPackage(packageName: String) = false

  override val allClasses get() = emptySet<String>()

  override val allPackages get() = emptySet<String>()

  override val allBundleNameSet: ResourceBundleNameSet get() = ResourceBundleNameSet(emptyMap())

  override fun toString() = "EmptyResolver"

  override fun close() = Unit

}
