package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.classes.utils.AsmUtil
import org.apache.commons.io.FileUtils
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.io.IOException
import java.util.*

class ClassFilesResolver(private val root: File) : Resolver() {

  private val allClasses = hashMapOf<String, File>()
  private val classPath = hashSetOf<File>()

  init {
    val classFiles = FileUtils.listFiles(root.canonicalFile, arrayOf("class"), true)
    for (classFile in classFiles) {
      val className = AsmUtil.readClassName(classFile)
      val classRoot = getClassRoot(classFile, className)
      if (classRoot != null) {
        allClasses.put(className, classFile)
        classPath.add(classRoot)
      }
    }
  }

  private fun getClassRoot(classFile: File, className: String): File? {
    val levelsUp = className.count { it == '/' }
    var root: File? = classFile
    for (i in 0 until levelsUp + 1) {
      root = if (root != null) root.parentFile else null
    }
    return root
  }

  @Throws(IOException::class)
  override fun findClass(className: String): ClassNode? {
    val file = allClasses[className] ?: return null
    return AsmUtil.readClassFromFile(file)
  }

  override fun getClassLocation(className: String): Resolver? = if (containsClass(className)) {
    this
  } else {
    null
  }

  override fun getAllClasses(): Set<String> = allClasses.keys

  override fun isEmpty(): Boolean = allClasses.isEmpty()

  override fun containsClass(className: String): Boolean = allClasses.containsKey(className)

  override fun getClassPath(): List<File> =  ArrayList(classPath)

  override fun getFinalResolvers(): List<Resolver> = listOf(this as Resolver)

  override fun close() {
    //nothing to do
  }

  override fun toString(): String = root.canonicalPath
}
