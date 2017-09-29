package com.jetbrains.plugin.structure.classes.resolvers

import com.google.common.collect.Iterators
import com.jetbrains.plugin.structure.base.utils.FileUtil
import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.classes.utils.AsmUtil
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.util.*
import java.util.jar.JarFile

class JarFileResolver(jar: File) : Resolver() {

  private val ioJarFile: File

  private val jarFile: JarFile

  private val classes: Set<String>

  init {
    if (!jar.exists()) {
      throw IllegalArgumentException("Jar file $jar doesn't exist")
    }
    if (!FileUtil.isJar(jar)) {
      throw IllegalArgumentException("File $jar is not a jar archive")
    }
    ioJarFile = jar

    jarFile = JarFile(ioJarFile)
    try {
      classes = readClasses()
    } catch (e: Throwable) {
      jarFile.closeLogged()
      throw e
    }
  }

  private fun readClasses(): Set<String> {
    val entries = jarFile.entries()
    val classes = HashSet<String>()
    while (entries.hasMoreElements()) {
      val entry = entries.nextElement()
      val entryName = entry.name
      if (entryName.endsWith(CLASS_SUFFIX)) {
        classes.add(entryName.substringBeforeLast(CLASS_SUFFIX))
      }
    }
    return classes
  }

  override fun getAllClasses(): Iterator<String> = Iterators.unmodifiableIterator<String>(classes.iterator())

  override fun toString(): String = ioJarFile.name

  override fun isEmpty(): Boolean = classes.isEmpty()

  override fun containsClass(className: String): Boolean = classes.contains(className)

  override fun getClassPath(): List<File> = listOf(ioJarFile)

  override fun getFinalResolvers(): List<Resolver> = listOf(this as Resolver)

  override fun findClass(className: String): ClassNode? =
      if (classes.contains(className)) evaluateNode(className) else null

  private fun evaluateNode(className: String): ClassNode? {
    val entry = jarFile.getEntry(className + CLASS_SUFFIX) ?: return null
    jarFile.getInputStream(entry).use { inputStream ->
      return AsmUtil.readClassNode(className, inputStream)
    }
  }

  override fun getClassLocation(className: String): Resolver? = if (containsClass(className)) this else null

  override fun close() {
    jarFile.close()
  }

  companion object {
    private val CLASS_SUFFIX = ".class"
  }
}
