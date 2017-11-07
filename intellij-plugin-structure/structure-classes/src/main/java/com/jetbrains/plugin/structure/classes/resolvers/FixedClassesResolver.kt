package com.jetbrains.plugin.structure.classes.resolvers

import org.objectweb.asm.tree.ClassNode
import java.io.File

class FixedClassesResolver(private val classes: Map<String, ClassNode>) : Resolver() {

  companion object {
    fun create(classes: List<ClassNode>): Resolver {
      return FixedClassesResolver(classes.asReversed().associateBy { it.name })
    }
  }

  override fun findClass(className: String): ClassNode? = classes[className]

  override fun getClassLocation(className: String): Resolver? = this

  override fun getAllClasses(): Set<String> = classes.keys

  override fun isEmpty(): Boolean = classes.isEmpty()

  override fun containsClass(className: String): Boolean = className in classes

  override fun getClassPath(): List<File> = emptyList()

  override fun getFinalResolvers(): List<Resolver> = listOf(this)

  override fun close() = Unit
}
