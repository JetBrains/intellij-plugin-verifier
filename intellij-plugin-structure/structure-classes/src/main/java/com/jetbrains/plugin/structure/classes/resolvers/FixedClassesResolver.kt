package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.classes.packages.PackageSet
import org.objectweb.asm.tree.ClassNode
import java.io.File

class FixedClassesResolver(private val classes: Map<String, ClassNode>) : Resolver() {
  companion object {
    fun create(classes: List<ClassNode>): Resolver {
      return FixedClassesResolver(classes.asReversed().associateBy { it.name })
    }
  }

  private val packageSet = PackageSet()

  init {
    for (className in classes.keys) {
      packageSet.addPackagesOfClass(className)
    }
  }

  override fun processAllClasses(processor: (ClassNode) -> Boolean) =
      classes.values
          .asSequence()
          .all(processor)

  override fun findClass(className: String): ClassNode? = classes[className]

  override fun getClassLocation(className: String): Resolver? = this

  override val allClasses
    get() = classes.keys

  override val allPackages: Set<String>
    get() = packageSet.getAllPackages()

  override val isEmpty
    get() = classes.isEmpty()

  override val classPath
    get() = emptyList<File>()

  override val finalResolvers
    get() = listOf(this)

  override fun containsClass(className: String) = className in classes

  override fun containsPackage(packageName: String) = packageSet.containsPackage(packageName)

  override fun close() = Unit

  override fun toString() = "Resolver of ${classes.size} predefined class" + (if (classes.size != 1) "es" else "")

}
