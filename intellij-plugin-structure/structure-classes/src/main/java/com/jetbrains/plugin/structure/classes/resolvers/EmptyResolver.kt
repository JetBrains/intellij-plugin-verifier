package com.jetbrains.plugin.structure.classes.resolvers

import org.objectweb.asm.tree.ClassNode
import java.io.File

object EmptyResolver : Resolver() {

  override fun findClass(className: String): ClassNode? = null

  override fun getClassLocation(className: String): Resolver? = null

  override fun getAllClasses(): Set<String> = emptySet()

  override fun isEmpty(): Boolean = true

  override fun containsClass(className: String): Boolean = false

  override fun getClassPath(): List<File> = emptyList()

  override fun getFinalResolvers(): List<Resolver> = emptyList()

  override fun toString(): String = "EmptyResolver"

  override fun close() {
    //do nothing
  }

}
