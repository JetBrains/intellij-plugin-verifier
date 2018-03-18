package com.jetbrains.plugin.structure.classes.resolvers

import org.objectweb.asm.tree.ClassNode
import java.io.File

class FixedClassesResolver(private val classes: Map<String, ClassNode>) : Resolver() {
  override fun processAllClasses(processor: (ClassNode) -> Boolean) =
      classes.values
          .asSequence()
          .all(processor)

  companion object {
    fun create(classes: List<ClassNode>): Resolver {
      return FixedClassesResolver(classes.asReversed().associateBy { it.name })
    }
  }

  override fun findClass(className: String): ClassNode? = classes[className]

  override fun getClassLocation(className: String): Resolver? = this

  override val allClasses
    get() = classes.keys

  override val isEmpty
    get() = classes.isEmpty()

  override val classPath
    get() = emptyList<File>()

  override val finalResolvers
    get() = listOf(this)

  override fun containsClass(className: String) = className in classes

  override fun close() = Unit

  override fun toString() = "Resolver of ${classes.size} predefined class" + (if (classes.size != 1) "es" else "")

}
