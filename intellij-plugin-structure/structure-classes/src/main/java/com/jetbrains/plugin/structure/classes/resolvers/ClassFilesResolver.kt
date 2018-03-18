package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.classes.utils.AsmUtil
import org.apache.commons.io.FileUtils
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.io.IOException

class ClassFilesResolver(private val root: File) : Resolver() {

  private val nameToClassFile = hashMapOf<String, File>()

  private val classPaths = linkedSetOf<File>()

  init {
    val classFiles = FileUtils.listFiles(root.canonicalFile, arrayOf("class"), true)
    for (classFile in classFiles) {
      val className = AsmUtil.readClassName(classFile)
      val classRoot = getClassRoot(classFile, className)
      if (classRoot != null) {
        nameToClassFile.put(className, classFile)
        classPaths.add(classRoot)
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
    val file = nameToClassFile[className] ?: return null
    return readClassNode(file)
  }

  private fun readClassNode(classFile: File) = AsmUtil.readClassFromFile(classFile)

  override fun getClassLocation(className: String): Resolver? = if (containsClass(className)) {
    this
  } else {
    null
  }

  override val allClasses
    get() = nameToClassFile.keys

  override val isEmpty
    get() = nameToClassFile.isEmpty()

  override fun containsClass(className: String) = className in nameToClassFile

  override val classPath
    get() = classPaths.toList()

  override val finalResolvers
    get() = listOf(this)

  override fun close() = Unit

  override fun processAllClasses(processor: (ClassNode) -> Boolean) =
      nameToClassFile.values
          .asSequence()
          .map { readClassNode(it) }
          .all { processor(it) }

  override fun toString() = root.canonicalPath!!
}
