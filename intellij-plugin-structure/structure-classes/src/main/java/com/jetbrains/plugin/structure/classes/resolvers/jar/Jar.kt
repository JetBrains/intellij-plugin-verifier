package com.jetbrains.plugin.structure.classes.resolvers.jar

import com.jetbrains.plugin.structure.base.utils.toSystemIndependentName
import com.jetbrains.plugin.structure.classes.resolvers.PackageSet
import com.jetbrains.plugin.structure.classes.utils.getBundleBaseName
import com.jetbrains.plugin.structure.classes.utils.getBundleNameByBundlePath
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

private const val CLASS_SUFFIX = ".class"

private const val PROPERTIES_SUFFIX = ".properties"

private const val SERVICE_PROVIDERS_PREFIX = "META-INF/services/"

class Jar(
  val jarPath: Path,
  private val fileSystemProvider: JarFileSystemProvider
) {

  private val classesInJar = mutableSetOf<ClassInJar>()

  val classes: Set<String> get() = classesInJar.mapTo(mutableSetOf()) { it.name }

  private val packageSet = PackageSet()

  val packages: Set<String>
    get() = packageSet.getAllPackages()

  private val _bundleNames = mutableMapOf<String, MutableSet<String>>()
  val bundleNames: Map<String, Set<String>> get() = _bundleNames

  private val _serviceProviders = mutableMapOf<String, Set<String>>()
  val serviceProviders: Map<String, Set<String>> get() = _serviceProviders

  fun init(): Jar = apply {
    fileSystemProvider.getFileSystem(jarPath).use { jarFs ->
      val jarRoot = jarFs.rootDirectories.single()
      Files.walk(jarRoot).use { paths ->
        paths.forEach { scan(it) }
      }
    }
  }

  fun processAllClasses(processor: (ClassInJar) -> Boolean): Boolean {
    return classesInJar.all { processor(it) }
  }

  fun containsPackage(packageName: String) = packageSet.containsPackage(packageName)

  fun containsClass(className: String) = className in classes

  fun getClassInJar(className: String): ClassInJar? = classesInJar.find { it.name == className }

  private fun scan(path: Path) {
    val pathInJar = getPathInJar(path)
    when {
      pathInJar.endsWith(CLASS_SUFFIX) -> handleClass(pathInJar, path)
      pathInJar.endsWith(PROPERTIES_SUFFIX) -> handleProperties(pathInJar)
      pathInJar.hasServiceProviders() -> handleServiceProvider(pathInJar, path)
    }
  }

  private fun handleClass(classInJar: String, path: Path) {
    val className = classInJar.substringBeforeLast(CLASS_SUFFIX)
    classesInJar += ClassInJar(className, path)
    packageSet.addPackagesOfClass(className)
  }

  private fun handleProperties(propertiesInJar: String) {
    val fullBundleName = getBundleNameByBundlePath(propertiesInJar)
    _bundleNames.getOrPut(getBundleBaseName(fullBundleName)) { mutableSetOf() } += fullBundleName
  }

  private fun handleServiceProvider(serviceProvidersInJar: String, metaInfServiceProviderPath: Path) {
    val serviceProvider = serviceProvidersInJar.substringAfter(SERVICE_PROVIDERS_PREFIX)
    _serviceProviders[serviceProvider] = readServiceImplementationNames(metaInfServiceProviderPath)
  }

  private fun readServiceImplementationNames(metaInfServiceProviderPath: Path): Set<String> {
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

  private fun String.hasServiceProviders() = startsWith(SERVICE_PROVIDERS_PREFIX) && count { it == '/' } == 2

  private fun getPathInJar(entry: Path): String =
    entry.toString().trimStart('/').toSystemIndependentName()

  data class ClassInJar(val name: String, val path: Path)
}

