/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.base.utils.*
import com.jetbrains.plugin.structure.classes.utils.AsmUtil
import com.jetbrains.plugin.structure.classes.utils.getBundleBaseName
import com.jetbrains.plugin.structure.classes.utils.getBundleNameByBundlePath
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.zip.ZipFile

class JarFileResolver(
  private val path: Path,
  override val readMode: ReadMode,
  private val fileOrigin: FileOrigin
) : Resolver() {

  private companion object {
    private const val CLASS_SUFFIX = ".class"

    private const val PROPERTIES_SUFFIX = ".properties"

    private const val SERVICE_PROVIDERS_PREFIX = "META-INF/services/"
  }

  private val classes: MutableSet<String> = hashSetOf()

  private val packageSet = PackageSet()

  private val bundleNames = hashMapOf<String, MutableSet<String>>()

  private val serviceProviders: MutableMap<String, Set<String>> = hashMapOf()

  private val zipFile: ZipFile

  init {
    require(path.exists()) { "File does not exist: $path" }
    require(path.simpleName.endsWith(".jar") || path.simpleName.endsWith(".zip")) { "File is neither a .jar nor .zip archive: $path" }
    zipFile = ZipFile(path.toFile())
    readClassNamesAndServiceProviders()
  }

  private fun readClassNamesAndServiceProviders() {
    for (entry in zipFile.entries().iterator()) {
      val entryName = entry.name.toSystemIndependentName()
      if (entryName.endsWith(CLASS_SUFFIX)) {
        val className = entryName.substringBeforeLast(CLASS_SUFFIX)
        classes.add(className)
        packageSet.addPackagesOfClass(className)
      } else if (entryName.endsWith(PROPERTIES_SUFFIX)) {
        val fullBundleName = getBundleNameByBundlePath(entryName)
        bundleNames.getOrPut(getBundleBaseName(fullBundleName)) { hashSetOf() } += fullBundleName
      } else if (!entry.isDirectory && entryName.startsWith(SERVICE_PROVIDERS_PREFIX) && entryName.count { it == '/' } == 2) {
        val serviceProvider = entryName.substringAfter(SERVICE_PROVIDERS_PREFIX)
        serviceProviders[serviceProvider] = readServiceImplementationNames(serviceProvider, zipFile)
      }
    }
  }

  private fun readServiceImplementationNames(serviceProvider: String, zipFile: ZipFile): Set<String> {
    val entry = SERVICE_PROVIDERS_PREFIX + serviceProvider
    val zipEntry = zipFile.getEntry(entry) ?: return emptySet()
    val lines = zipFile.getInputStream(zipEntry).reader().readLines()
    return lines.map { it.substringBefore("#").trim() }.filterNotTo(hashSetOf()) { it.isEmpty() }
  }

  val implementedServiceProviders: Map<String, Set<String>>
    get() = serviceProviders

  override val allPackages
    get() = packageSet.getAllPackages()

  override val allBundleNameSet: ResourceBundleNameSet
    get() = ResourceBundleNameSet(bundleNames)

  override val allClasses
    get() = classes

  override fun processAllClasses(processor: (ClassNode) -> Boolean): Boolean {
    for (zipEntry in zipFile.entries().iterator()) {
      val entryName = zipEntry.name
      if (entryName.endsWith(CLASS_SUFFIX)) {
        val className = entryName.substringBeforeLast(CLASS_SUFFIX)
        zipFile.getInputStream(zipEntry).use {
          val classNode = AsmUtil.readClassNode(className, it, readMode == ReadMode.FULL)
          if (!processor(classNode)) {
            return false
          }
        }
      }
    }
    return true
  }

  override fun containsClass(className: String) = className in classes

  override fun containsPackage(packageName: String) = packageSet.containsPackage(packageName)

  override fun resolveClass(className: String): ResolutionResult<ClassNode> {
    if (className !in classes) {
      return ResolutionResult.NotFound
    }
    try {
      val classNode = readClassNode(className) ?: return ResolutionResult.NotFound
      return ResolutionResult.Found(classNode, fileOrigin)
    } catch (e: InvalidClassFileException) {
      return ResolutionResult.Invalid(e.message)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      return ResolutionResult.FailedToRead(e.message ?: e.javaClass.name)
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

  private fun readPropertyResourceBundle(bundleResourceName: String): PropertyResourceBundle? {
    val resourceEntry = zipFile.getEntry(bundleResourceName) ?: return null
    return zipFile.getInputStream(resourceEntry).use {
      PropertyResourceBundle(it)
    }
  }

  private fun readClassNode(className: String): ClassNode? {
    val entry = zipFile.getEntry(className + CLASS_SUFFIX) ?: return null
    return zipFile.getInputStream(entry).use {
      AsmUtil.readClassNode(className, it, readMode == ReadMode.FULL)
    }
  }

  override fun close() = zipFile.close()

  override fun toString() = path.toAbsolutePath().toString()

}

fun buildJarOrZipFileResolvers(
  jarsOrZips: Iterable<File>,
  readMode: Resolver.ReadMode,
  parentOrigin: FileOrigin
): List<Resolver> {
  val resolvers = arrayListOf<Resolver>()
  resolvers.closeOnException {
    jarsOrZips.mapTo(resolvers) { file ->
      val fileOrigin = JarOrZipFileOrigin(file.name, parentOrigin)
      JarFileResolver(file.toPath(), readMode, fileOrigin)
    }
  }
  return resolvers
}