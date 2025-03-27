package com.jetbrains.plugin.structure.classes.resolvers.jar

import com.jetbrains.plugin.structure.base.utils.extension
import com.jetbrains.plugin.structure.base.utils.isFile
import com.jetbrains.plugin.structure.classes.resolvers.PackageSet
import com.jetbrains.plugin.structure.classes.resolvers.jar.Jar.Element.Other
import com.jetbrains.plugin.structure.classes.utils.getBundleBaseName
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import com.jetbrains.plugin.structure.jar.META_INF
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

private val LOG: Logger = LoggerFactory.getLogger(Jar::class.java)

private const val CLASS_EXTENSION = "class"
private const val CLASS_SUFFIX = ".$CLASS_EXTENSION"

private const val RESOURCE_BUNDLE_EXTENSION = "properties"
private const val RESOURCE_BUNDLE_SUFFIX = ".$RESOURCE_BUNDLE_EXTENSION"

private const val JAR_PATH_SEPARATOR_CHAR = '/'

private const val RESOURCE_BUNDLE_SEPARATOR = '.'

class Jar(
  val jarPath: Path,
  private val fileSystemProvider: JarFileSystemProvider
) : AutoCloseable {

  private val classesInJar = mutableSetOf<ClassInJar>()

  val classes: Set<String> get() = classesInJar.mapTo(mutableSetOf()) { it.name }

  val packages: Set<String>
    get() = getAllPackages()

  private val _bundleNames = mutableMapOf<String, MutableSet<String>>()
  val bundleNames: Map<String, Set<String>> get() = _bundleNames

  private val _serviceProviders = mutableMapOf<String, Set<String>>()
  val serviceProviders: Map<String, Set<String>> get() = _serviceProviders

  fun init(): Jar = apply {
    val jarFs = fileSystemProvider.getFileSystem(jarPath)
    val jarRoot = jarFs.rootDirectories.single()
    Files.walk(jarRoot).use { paths ->
      paths.forEach { scan(it) }
    }
  }

  fun processAllClasses(processor: (ClassInJar) -> Boolean): Boolean {
    return classesInJar.all { processor(it) }
  }

  fun containsPackage(packageName: String) = packages.contains(packageName)

  fun containsClass(className: String) = className in classes

  fun getClassInJar(className: String): ClassInJar? = classesInJar.find { it.name == className }

  override fun close() {
    fileSystemProvider.close(jarPath)
  }

  private fun scan(path: Path) {
    when (val element = resolve(path)) {
      is Element.Class -> handle(element)
      is Element.ResourceBundle -> handle(element)
      is Element.ServiceProvider -> handle(element)
      Other -> Unit
    }
  }

  private fun resolve(path: Path): Element {
    val ext = path.extension
    return if (ext == CLASS_EXTENSION) {
      Element.Class(resolveClass(path), path)
    } else if (ext == RESOURCE_BUNDLE_EXTENSION) {
      Element.ResourceBundle(resolveBundleName(path))
    } else if (path.hasServiceProviders()) {
      Element.ServiceProvider(path.getServiceProvider(), path)
    } else {
      Other
    }
  }

  private fun handle(classElement: Element.Class) {
    classesInJar += ClassInJar(classElement.name, classElement.path)
  }

  private fun handle(resourceBundle: Element.ResourceBundle) {
    val fullBundleName = resourceBundle.name
    _bundleNames.getOrPut(getBundleBaseName(fullBundleName)) { mutableSetOf() } += fullBundleName
  }

  private fun handle(serviceProvider: Element.ServiceProvider) {
    _serviceProviders[serviceProvider.name] = readServiceImplementationNames(serviceProvider.path)
  }

  private fun readServiceImplementationNames(metaInfServiceProviderPath: Path): Set<String> {
    if (!metaInfServiceProviderPath.isParsableServiceImplementation()) {
      return emptySet()
    }
    return Files.lines(metaInfServiceProviderPath).use { lines ->
      lines.asSequence()
        .mapNotNull { parseServiceImplementationLine(it) }
        .toSet()
    }
  }

  private fun parseServiceImplementationLine(line: String): String? {
    val serviceImplementation = line.substringBefore("#").trim()
    return serviceImplementation.takeIf { it.isNotEmpty() }
  }

  private fun resolve(path: Path, separator: Char, suffix: String): String {
    return path.toString().replace(File.separatorChar, separator)
      .removePrefix(separator.toString())
      .removeSuffix(suffix)

  }

  private fun resolveClass(path: Path): String {
    return resolve(path, JAR_PATH_SEPARATOR_CHAR, CLASS_SUFFIX)
  }

  private fun resolveBundleName(path: Path): String {
    return resolve(path, RESOURCE_BUNDLE_SEPARATOR, RESOURCE_BUNDLE_SUFFIX)
  }

  private fun Path.hasServiceProviders(): Boolean {
    if (nameCount != 3) {
      // 0: META-INF, 1: services, 2: provider name
      return false
    }
    return getName(0).toString() == META_INF && getName(1).toString() == "services"
  }

  private fun Path.getServiceProvider(): String {
    return fileName.toString()
  }

  private fun Path.isParsableServiceImplementation(): Boolean {
    if (!Files.exists(this)) {
      LOG.debug("Service provider file {} does not exist", this)
      return false
    }
    if (!isFile) {
      LOG.debug("Service provider file {} is not a regular file", this)
      return false
    }
    return true
  }

  private fun getAllPackages(): Set<String> {
    return PackageSet().apply {
      classesInJar.forEach { addPackagesOfClass(it.name) }
    }.getAllPackages()
  }

  data class ClassInJar(val name: String, val path: Path)

  sealed class Element {
    data class Class(val name: String, val path: Path) : Element()
    data class ResourceBundle(val name: String) : Element()
    data class ServiceProvider(val name: String, val path: Path) : Element()
    object Other : Element()
  }
}

