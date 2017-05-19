package com.jetbrains.pluginverifier.utils

import com.intellij.structure.resolvers.Resolver
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode
import java.io.File

/**
 * @author Sergey Patrikeev
 */
class CloseIgnoringResolver(private val delegate: Resolver) : Resolver() {
  override fun containsClass(className: String?): Boolean = delegate.containsClass(className)

  override fun isEmpty(): Boolean = delegate.isEmpty

  override fun findClass(className: String?): ClassNode = delegate.findClass(className)

  override fun getClassLocation(className: String?): Resolver = delegate.getClassLocation(className)

  override fun getAllClasses(): Iterator<String> = delegate.allClasses

  override fun getClassPath(): MutableList<File> = delegate.classPath

  override fun close(): Unit = Unit

}