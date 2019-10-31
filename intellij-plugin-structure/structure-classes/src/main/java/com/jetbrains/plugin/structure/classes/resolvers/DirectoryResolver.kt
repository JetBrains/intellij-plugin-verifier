package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.base.utils.closeOnException
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.base.utils.toSystemIndependentName
import com.jetbrains.plugin.structure.classes.utils.AsmUtil
import com.jetbrains.plugin.structure.classes.utils.getBundleBaseName
import com.jetbrains.plugin.structure.classes.utils.getBundleNameByBundlePath
import org.apache.commons.io.FileUtils
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.nio.file.Path
import java.util.*

class DirectoryResolver(
  private val root: Path,
  private val fileOrigin: FileOrigin,
  override val readMode: ReadMode = ReadMode.FULL
) : Resolver() {

  private val classNameToFile = hashMapOf<String, File>()

  private val bundlePathToFile = hashMapOf<String, File>()

  private val bundleNames = hashMapOf<String, MutableSet<String>>()

  private val packageSet = PackageSet()

  init {
    val canonicalRoot = root.toFile().canonicalFile
    val files = FileUtils.listFiles(canonicalRoot, arrayOf("class", "properties"), true)
    for (file in files) {
      if (file.extension == "class") {
        val className = AsmUtil.readClassName(file)
        val classRoot = getClassRoot(file, className)
        if (classRoot != null) {
          classNameToFile[className] = file
          packageSet.addPackagesOfClass(className)
        }
      }
      if (file.extension == "properties") {
        val bundlePath = file.relativeTo(canonicalRoot).path.toSystemIndependentName()
        bundlePathToFile[bundlePath] = file
        val fullBundleName = getBundleNameByBundlePath(bundlePath)
        bundleNames.getOrPut(getBundleBaseName(fullBundleName)) { hashSetOf() } += fullBundleName
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
    val classFile = classNameToFile[className] ?: return ResolutionResult.NotFound
    val classNode = try {
      AsmUtil.readClassFromFile(className, classFile, readMode == ReadMode.FULL)
    } catch (e: InvalidClassFileException) {
      return ResolutionResult.Invalid(e.message)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      return ResolutionResult.FailedToRead(e.message ?: e.javaClass.name)
    }
    return ResolutionResult.Found(classNode, fileOrigin)
  }

  override fun resolveExactPropertyResourceBundle(baseName: String, locale: Locale): ResolutionResult<PropertyResourceBundle> {
    val control = ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_PROPERTIES)
    val bundleName = control.toBundleName(baseName, locale)
    val bundlePath = control.toResourceName(bundleName, "properties")
    val bundleFile = bundlePathToFile[bundlePath]
    if (bundleFile != null) {
      val propertyResourceBundle: PropertyResourceBundle = try {
        PropertyResourceBundle(bundleFile.bufferedReader())
      } catch (e: IllegalArgumentException) {
        return ResolutionResult.Invalid(e.message ?: e.javaClass.name)
      } catch (e: Exception) {
        return ResolutionResult.FailedToRead(e.message ?: e.javaClass.name)
      }
      return ResolutionResult.Found(propertyResourceBundle, fileOrigin)
    }
    return ResolutionResult.NotFound
  }

  override val allPackages
    get() = packageSet.getAllPackages()

  override val allBundleNameSet: ResourceBundleNameSet
    get() = ResourceBundleNameSet(bundleNames)

  override val allClasses
    get() = classNameToFile.keys

  override fun containsClass(className: String) = className in classNameToFile

  override fun containsPackage(packageName: String) = packageSet.containsPackage(packageName)

  override fun close() = Unit

  override fun processAllClasses(processor: (ClassNode) -> Boolean): Boolean {
    for ((className, classFile) in classNameToFile) {
      val classNode = AsmUtil.readClassFromFile(className, classFile, readMode == ReadMode.FULL)
      if (!processor(classNode)) {
        return false
      }
    }
    return true
  }

  override fun toString() = root.toAbsolutePath().toString()
}

fun buildDirectoriesResolvers(
  directories: Iterable<Path>,
  readMode: Resolver.ReadMode,
  parentOrigin: FileOrigin
): List<Resolver> {
  val resolvers = arrayListOf<Resolver>()
  resolvers.closeOnException {
    directories.mapTo(resolvers) { directory ->
      val fileOrigin = DirectoryFileOrigin(directory.simpleName, parentOrigin)
      DirectoryResolver(directory, fileOrigin, readMode)
    }
  }
  return resolvers
}