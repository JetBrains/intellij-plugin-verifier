/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.base.utils.closeOnException
import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.inputStream
import com.jetbrains.plugin.structure.base.utils.readLines
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.base.utils.toSystemIndependentName
import com.jetbrains.plugin.structure.classes.utils.getBundleBaseName
import com.jetbrains.plugin.structure.classes.utils.getBundleNameByBundlePath
import org.objectweb.asm.tree.ClassNode
import java.nio.file.FileSystem
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class JarFileResolver(
  override val jarPath: Path,
  override val readMode: ReadMode,
  override val fileOrigin: FileOrigin
) : AbstractJarResolver(jarPath, readMode, fileOrigin) {

  private companion object {
    private const val CLASS_SUFFIX = ".class"

    private const val PROPERTIES_SUFFIX = ".properties"

    private const val SERVICE_PROVIDERS_PREFIX = "META-INF/services/"
  }

  private val classes: MutableSet<String> = hashSetOf()

  private val packageSet = PackageSet()

  override val bundleNames = hashMapOf<String, MutableSet<String>>()

  private val serviceProviders: MutableMap<String, Set<String>> = hashMapOf()

  private val isClosed = AtomicBoolean()

  init {
    JarFileSystemsPool.checkIsJar(jarPath)
    JarFileSystemsPool.perform(jarPath) { jarFs ->
      readClassNamesAndServiceProviders(jarFs)
    }
  }

  private fun readClassNamesAndServiceProviders(jarFs: FileSystem) {
    val jarRoot = jarFs.rootDirectories.single()
    val visitedDirs = hashSetOf<Path>()
    Files.walkFileTree(jarRoot, object : SimpleFileVisitor<Path>() {
      override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes?): FileVisitResult {
        if (!visitedDirs.add(dir)) {
          return FileVisitResult.SKIP_SUBTREE
        }
        return FileVisitResult.CONTINUE
      }

      override fun visitFile(file: Path, attrs: BasicFileAttributes?): FileVisitResult {
        val entryName = getPathInJar(file)
        when {
          entryName.endsWith(CLASS_SUFFIX) -> {
            val className = entryName.substringBeforeLast(CLASS_SUFFIX)
            classes.add(className)
            packageSet.addPackagesOfClass(className)
          }
          entryName.endsWith(PROPERTIES_SUFFIX) -> {
            val fullBundleName = getBundleNameByBundlePath(entryName)
            bundleNames.getOrPut(getBundleBaseName(fullBundleName)) { hashSetOf() } += fullBundleName
          }
          entryName.startsWith(SERVICE_PROVIDERS_PREFIX) && entryName.count { it == '/' } == 2 -> {
            val serviceProvider = entryName.substringAfter(SERVICE_PROVIDERS_PREFIX)
            serviceProviders[serviceProvider] = readServiceImplementationNames(serviceProvider, jarFs)
          }
        }
        return FileVisitResult.CONTINUE
      }
    })
  }

  private fun getPathInJar(entry: Path): String =
    entry.toString().trimStart('/').toSystemIndependentName()

  private fun readServiceImplementationNames(serviceProvider: String, jarFs: FileSystem): Set<String> {
    val entry = SERVICE_PROVIDERS_PREFIX + serviceProvider
    val entryPath = jarFs.getPath(entry)
    if (!entryPath.exists()) {
      return emptySet()
    }
    return entryPath.readLines().map { it.substringBefore("#").trim() }.filterNotTo(hashSetOf()) { it.isEmpty() }
  }

  override val implementedServiceProviders: Map<String, Set<String>>
    get() = serviceProviders

  override val allPackages
    get() = packageSet.getAllPackages()

  override val allBundleNameSet: ResourceBundleNameSet
    get() = ResourceBundleNameSet(bundleNames)

  override val allClasses
    get() = classes

  override fun processAllClasses(processor: (ResolutionResult<ClassNode>) -> Boolean): Boolean {
    checkIsOpen()
    return JarFileSystemsPool.perform(jarPath) { jarFs ->
      Files.walk(jarFs.rootDirectories.single()).use { stream ->
        for (jarEntry in stream.filter { it.simpleName.endsWith(CLASS_SUFFIX) }) {
          val className = getPathInJar(jarEntry).removeSuffix(CLASS_SUFFIX)
          val result = readClass(className, jarEntry)
          if (!processor(result)) {
            return@use false
          }
        }
        return@use true
      }
    }
  }

  override fun containsClass(className: String) = className in classes

  override fun containsPackage(packageName: String) = packageSet.containsPackage(packageName)

  override fun resolveClass(className: String): ResolutionResult<ClassNode> {
    checkIsOpen()
    if (className !in classes) {
      return ResolutionResult.NotFound
    }
    return JarFileSystemsPool.perform(jarPath) { jarFs ->
      val classPath = jarFs.getPath(className + CLASS_SUFFIX)
      if (classPath.exists()) {
        readClass(className, classPath)
      } else {
        ResolutionResult.NotFound
      }
    }
  }

  override fun resolveExactPropertyResourceBundle(baseName: String, locale: Locale): ResolutionResult<PropertyResourceBundle> {
    if (baseName !in bundleNames) {
      return ResolutionResult.NotFound
    }

    val control = ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_PROPERTIES)
    val bundleName = control.toBundleName(baseName, locale)

    val resourceName = control.toResourceName(bundleName, "properties")
    val propertyResourceBundle = try {
      readPropertyResourceBundle(resourceName)
    } catch (e: IllegalArgumentException) {
      return ResolutionResult.Invalid(e.message ?: e.javaClass.name)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      return ResolutionResult.FailedToRead(e.message ?: e.javaClass.name)
    }

    if (propertyResourceBundle != null) {
      return ResolutionResult.Found(propertyResourceBundle, fileOrigin)
    }

    return ResolutionResult.NotFound
  }

  override fun readPropertyResourceBundle(bundleResourceName: String): PropertyResourceBundle? {
    checkIsOpen()
    return JarFileSystemsPool.perform(jarPath) { jarFs ->
      val path = jarFs.getPath(bundleResourceName)
      if (path.exists()) {
        path.inputStream().use { PropertyResourceBundle(it) }
      } else {
        null
      }
    }
  }

  private fun checkIsOpen() {
    check(!isClosed.get()) { "Jar file system must be open for $this" }
  }

  override fun close() {
    if (isClosed.compareAndSet(false, true)) {
      JarFileSystemsPool.close(jarPath)
    }
  }

  override fun toString() = jarPath.toAbsolutePath().toString()

}

fun buildJarOrZipFileResolvers(
  jarsOrZips: Iterable<Path>,
  readMode: Resolver.ReadMode,
  parentOrigin: FileOrigin
): List<Resolver> {
  val resolvers = arrayListOf<Resolver>()
  resolvers.closeOnException {
    jarsOrZips.mapTo(resolvers) { file ->
      val fileOrigin = JarOrZipFileOrigin(file.simpleName, parentOrigin)
      LazyJarResolver(file, readMode, fileOrigin)
    }
  }
  return resolvers
}