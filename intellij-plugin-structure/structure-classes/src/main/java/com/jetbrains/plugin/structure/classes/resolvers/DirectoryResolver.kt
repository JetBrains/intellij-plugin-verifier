package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.classes.utils.AsmUtil
import org.apache.commons.io.FileUtils
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.nio.file.Path

class DirectoryResolver(
    private val root: Path,
    private val classFileOrigin: FileOrigin,
    override val readMode: ReadMode = ReadMode.FULL
) : Resolver() {

  private val nameToClassFile = hashMapOf<String, File>()

  private val packageSet = PackageSet()

  init {
    val classFiles = FileUtils.listFiles(root.toFile().canonicalFile, arrayOf("class"), true)
    for (classFile in classFiles) {
      val className = AsmUtil.readClassName(classFile)
      val classRoot = getClassRoot(classFile, className)
      if (classRoot != null) {
        nameToClassFile[className] = classFile
        packageSet.addPackagesOfClass(className)
      }
    }
  }

  private fun getClassRoot(classFile: File, className: String): File? {
    val levelsUp = className.count { it == '/' }
    var root: File? = classFile
    for (i in 0 until levelsUp + 1) {
      root = root?.parentFile
    }
    return root
  }

  override fun resolveClass(className: String): ResolutionResult<ClassNode> {
    val classFile = nameToClassFile[className] ?: return ResolutionResult.NotFound
    val classNode = try {
      AsmUtil.readClassFromFile(className, classFile, readMode == ReadMode.FULL)
    } catch (e: InvalidClassFileException) {
      return ResolutionResult.Invalid(e.message)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      return ResolutionResult.FailedToRead(e.localizedMessage ?: e.javaClass.name)
    }
    return ResolutionResult.Found(classNode, classFileOrigin)
  }

  override val allPackages
    get() = packageSet.getAllPackages()

  override val allClasses
    get() = nameToClassFile.keys

  override val isEmpty
    get() = nameToClassFile.isEmpty()

  override fun containsClass(className: String) = className in nameToClassFile

  override fun containsPackage(packageName: String) = packageSet.containsPackage(packageName)

  override fun close() = Unit

  override fun processAllClasses(processor: (ClassNode) -> Boolean): Boolean {
    for ((className, classFile) in nameToClassFile) {
      val classNode = AsmUtil.readClassFromFile(className, classFile, readMode == ReadMode.FULL)
      if (!processor(classNode)) {
        return false
      }
    }
    return true
  }

  override fun toString() = root.toAbsolutePath().toString()
}
