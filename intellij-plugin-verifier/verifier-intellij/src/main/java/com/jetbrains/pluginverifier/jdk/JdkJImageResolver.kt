/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.jdk

import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.classes.resolvers.*
import com.jetbrains.plugin.structure.classes.utils.AsmUtil
import org.objectweb.asm.tree.ClassNode
import java.net.URI
import java.nio.file.*
import java.util.*
import java.util.stream.Collectors

/**
 * [Resolver] that reads class files from JImage corresponding to `<JDK>/lib/modules` file of JDK 9 and later.
 */
class JdkJImageResolver(jdkPath: Path, override val readMode: ReadMode) : Resolver() {
  private companion object {

    val JRT_SCHEME_URI: URI = URI.create("jrt:/")
  }

  private val fileOrigin: FileOrigin = JdkFileOrigin(jdkPath)

  private val classNameToModuleName: Map<String, String>

  private val packageSet = PackageSet()

  private val nameSeparator: String

  private val modulesPath: Path

  private val fileSystem: FileSystem

  init {
    fileSystem = try {
      getJrtFileSystem(jdkPath)
    } catch (e: Exception) {
      throw RuntimeException("Unable to read content from jrt:/ file system.", e)
    }

    nameSeparator = fileSystem.separator
    modulesPath = fileSystem.getPath("/modules")

    classNameToModuleName = Files.walk(modulesPath).use { stream ->
      stream
        .filter { p -> p.fileName.toString().endsWith(".class") }
        .collect(
          Collectors.toMap(
            { p -> getClassName(p) },
            { p -> getModuleName(p) },
            { one, _ -> one }
          )
        )
    }

    for (className in classNameToModuleName.keys) {
      packageSet.addPackagesOfClass(className)
    }
  }

  private fun getJrtFileSystem(javaHome: Path): FileSystem {
    return FileSystems.newFileSystem(JRT_SCHEME_URI, mapOf("java.home" to javaHome.toString()))
  }

  private fun getModuleName(classPath: Path): String =
    modulesPath.relativize(classPath).first().toString()

  private fun getClassName(classPath: Path): String {
    val relative = modulesPath.relativize(classPath)
    return relative
      .subpath(1, relative.nameCount).toString()
      .substringBeforeLast(".class").replace(nameSeparator, "/")
  }

  override val allClasses
    get() = classNameToModuleName.keys

  override val allPackages
    get() = packageSet.getAllPackages()

  override val allBundleNameSet
    get() = ResourceBundleNameSet(emptyMap())

  /**
   * Resolve class with specified binary name.
   *
   * The class name must be slash separated, e.g. `java/lang/String`.
   */
  override fun resolveClass(className: String): ResolutionResult<ClassNode> {
    val moduleName = classNameToModuleName[className]
    if (moduleName != null) {
      val classPath = modulesPath.resolve(moduleName).resolve(className.replace("/", nameSeparator) + ".class")
      return readClass(className, classPath)
    }
    return ResolutionResult.NotFound
  }

  private fun readClass(className: String, classPath: Path): ResolutionResult<ClassNode> =
    try {
      val classNode = readClassNode(className, classPath)
      ResolutionResult.Found(classNode, fileOrigin)
    } catch (e: InvalidClassFileException) {
      ResolutionResult.Invalid(e.message)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      ResolutionResult.FailedToRead(e.localizedMessage ?: e.javaClass.name)
    }

  override fun resolveExactPropertyResourceBundle(baseName: String, locale: Locale) = ResolutionResult.NotFound

  private fun readClassNode(className: String, classFilePath: Path): ClassNode =
    Files.newInputStream(classFilePath, StandardOpenOption.READ).use { inputStream ->
      AsmUtil.readClassNode(className, inputStream, readMode == ReadMode.FULL)
    }

  override fun containsClass(className: String) = className in classNameToModuleName

  override fun containsPackage(packageName: String) = packageSet.containsPackage(packageName)

  override fun processAllClasses(processor: (ResolutionResult<ClassNode>) -> Boolean): Boolean {
    Files.walk(modulesPath).use { stream ->
      for (classPath in stream.filter { it.fileName.toString().endsWith(".class") }) {
        val className = getClassName(classPath)
        val result = readClass(className, classPath)
        if (!processor(result)) {
          return false
        }
      }
    }
    return true
  }

  override fun close() {
    fileSystem.close()
  }
}