/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.base.BinaryClassName
import com.jetbrains.plugin.structure.base.utils.closeOnException
import com.jetbrains.plugin.structure.base.utils.extension
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.base.utils.toSystemIndependentName
import com.jetbrains.plugin.structure.classes.utils.AsmUtil
import com.jetbrains.plugin.structure.classes.utils.getBundleBaseName
import com.jetbrains.plugin.structure.classes.utils.getBundleNameByBundlePath
import org.objectweb.asm.tree.ClassNode
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class DirectoryResolver(
  private val root: Path,
  private val fileOrigin: FileOrigin,
  override val readMode: ReadMode = ReadMode.FULL
) : Resolver() {

  private val classNameToFile = hashMapOf<String, Path>()

  private val bundlePathToFile = hashMapOf<String, Path>()

  private val bundleNames = hashMapOf<String, MutableSet<String>>()

  private val packageSet = Packages()

  init {
    Files.walk(root).use { fileStream ->
      fileStream.forEach { file ->
        if (file.extension == "class") {
          val className = AsmUtil.readClassName(file)
          val classRoot = getClassRoot(file, className)
          if (classRoot != null) {
            classNameToFile[className] = file
            packageSet.addClass(className)
          }
        }
        if (file.extension == "properties") {
          val bundlePath = root.relativize(file).toString().toSystemIndependentName()
          bundlePathToFile[bundlePath] = file
          val fullBundleName = getBundleNameByBundlePath(bundlePath)
          bundleNames.getOrPut(getBundleBaseName(fullBundleName)) { hashSetOf() } += fullBundleName
        }
      }
    }
  }

  private fun getClassRoot(classFile: Path, className: String): Path? {
    val levelsUp = className.count { it == '/' }
    var root: Path? = classFile
    for (i in 0 until levelsUp + 1) {
      root = root?.parent
    }
    return root
  }

  override fun resolveClass(className: String): ResolutionResult<ClassNode> {
    val classFile = classNameToFile[className] ?: return ResolutionResult.NotFound
    return readClass(className, classFile)
  }

  private fun readClass(className: String, classFile: Path): ResolutionResult<ClassNode> =
    try {
      val classNode = AsmUtil.readClassFromFile(className, classFile, readMode == ReadMode.FULL)
      ResolutionResult.Found(classNode, fileOrigin)
    } catch (e: InvalidClassFileException) {
      ResolutionResult.Invalid(e.message)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      ResolutionResult.FailedToRead(e.message ?: e.javaClass.name)
    }

  override fun resolveExactPropertyResourceBundle(baseName: String, locale: Locale): ResolutionResult<PropertyResourceBundle> {
    val control = ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_PROPERTIES)
    val bundleName = control.toBundleName(baseName, locale)
    val bundlePath = control.toResourceName(bundleName, "properties")
    val bundleFile = bundlePathToFile[bundlePath]
    if (bundleFile != null) {
      val propertyResourceBundle: PropertyResourceBundle = try {
        PropertyResourceBundle(Files.newInputStream(bundleFile).bufferedReader())
      } catch (e: IllegalArgumentException) {
        return ResolutionResult.Invalid(e.message ?: e.javaClass.name)
      } catch (e: Exception) {
        return ResolutionResult.FailedToRead(e.message ?: e.javaClass.name)
      }
      return ResolutionResult.Found(propertyResourceBundle, fileOrigin)
    }
    return ResolutionResult.NotFound
  }

  @Deprecated("Use 'packages' property instead. This property may be slow on some file systems.")
  override val allPackages
    get() = packageSet.all

  override val packages: Set<String>
    get() = packageSet.entries

  override val allBundleNameSet: ResourceBundleNameSet
    get() = ResourceBundleNameSet(bundleNames)

  override val allClasses
    get() = classNameToFile.keys

  override val allClassNames: Set<BinaryClassName>
    get() = allClasses

  override fun containsClass(className: String) = className in classNameToFile

  override fun containsPackage(packageName: String) = packageName in packageSet

  override fun close() = Unit

  override fun processAllClasses(processor: (ResolutionResult<ClassNode>) -> Boolean): Boolean {
    for ((className, classFile) in classNameToFile) {
      val result = readClass(className, classFile)
      if (!processor(result)) {
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