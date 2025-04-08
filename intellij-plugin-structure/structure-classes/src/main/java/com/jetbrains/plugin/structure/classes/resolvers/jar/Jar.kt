package com.jetbrains.plugin.structure.classes.resolvers.jar

import com.jetbrains.plugin.structure.base.utils.isFile
import com.jetbrains.plugin.structure.classes.resolvers.PackageSet
import com.jetbrains.plugin.structure.classes.resolvers.jar.Jar.Element.Other
import com.jetbrains.plugin.structure.classes.utils.getBundleBaseName
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import com.jetbrains.plugin.structure.jar.invoke
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import kotlin.streams.asSequence

private val LOG: Logger = LoggerFactory.getLogger(Jar::class.java)

private const val CLASS_EXTENSION = "class"
private const val CLASS_SUFFIX = ".$CLASS_EXTENSION"

private const val RESOURCE_BUNDLE_EXTENSION = "properties"
private const val RESOURCE_BUNDLE_SUFFIX = ".$RESOURCE_BUNDLE_EXTENSION"

private const val JAR_PATH_SEPARATOR_CHAR = '/'

private const val RESOURCE_BUNDLE_SEPARATOR = '.'

typealias PathInJar = String

class Jar(
  val jarPath: Path,
  private val fileSystemProvider: JarFileSystemProvider
) : AutoCloseable {

  private val classesInJar = hashSetOf<ClassInJar>()

  val classes: Set<String> get() = classesInJar.mapTo(mutableSetOf()) { it.name }

  val packages: Set<String>
    get() = getAllPackages()

  val packageSet: Set<String> by lazy {
    classesInJar.mapTo(mutableSetOf()) { it.name.substringBeforeLast('/', "")
      it.name.substringBeforeLast('/', "")
    }
  }

  private val _bundleNames = mutableMapOf<String, MutableSet<String>>()
  val bundleNames: Map<String, Set<String>> get() = _bundleNames

  val serviceProviders: Map<String, Set<String>> by lazy {
    val serviceProviders = mutableMapOf<String, MutableSet<String>>()
    try {
      val fs = fileSystemProvider.getFileSystem(jarPath)
      serviceProviderPaths.forEach { spPath ->
        val serviceProviderFile = fs.getPath(spPath)
        if (serviceProviderFile.isFile) {
          val serviceProviderNames = readServiceImplementationNames(serviceProviderFile)
          serviceProviders.getOrPut(serviceProviderFile.fileName.toString()) { mutableSetOf() } += serviceProviderNames
        }
      }
    } finally {
      fileSystemProvider.close(jarPath)
    }
    serviceProviders
  }

  private val serviceProviderPaths = mutableSetOf<String>()

  fun init(): Jar = apply {
    if (jarPath.supportsFile()) {
      init(jarPath.toFile())
    } else {
      init(jarPath)
    }
  }

  fun processAllClasses(processor: (String, Path) -> Boolean): Boolean {
    return useFileSystem{ fs ->
      classesInJar.all { (className, classFilePath) ->
        fs.getPath(classFilePath)
          .takeIf { it.isFile }
          ?.let { processor(className, it) } == true
      }
    }
  }

  private fun <T> useFileSystem(action: (FileSystem) -> T): T {
    return fileSystemProvider(jarPath, action)
  }

  fun containsPackage(packageName: String) = packages.contains(packageName)

  fun containsClass(className: String) = className in classes

  fun getClassInJar(className: String): ClassInJar? = classesInJar.find { it.name == className }

  fun <T> withClass(className: String, handler: (String, Path) -> T): T? {
    return getClassInJar(className)?.let {  (_, pathInJar) ->
      useFileSystem {
        it.getPath(pathInJar)
          .takeIf { it.isFile }
          ?.let {
            handler(className, it)
          }
      }
    }
  }

  override fun close() {
    // NO-OP
  }

  private fun Path.supportsFile() = fileSystem == FileSystems.getDefault()

  private fun init(jarPath: File) {
    ZipFile(jarPath).use { zip ->
      zip.entries().asIterator().forEach {
        if (!it.isDirectory) {
          scan(it)
        }
      }
    }
  }

  private fun init(jarPath: Path) {
    Files.newInputStream(jarPath).use { it ->
      ZipInputStream(it).use { jar ->
        var zipEntry = jar.nextEntry
        while (zipEntry != null) {
          if (!zipEntry.isDirectory) {
            scan(zipEntry)
          }
          jar.closeEntry()
          zipEntry = jar.nextEntry
        }
      }
    }
  }

  private fun resolve(zipEntry: ZipEntry): Element {
    val path = zipEntry.name.normalizePath()
    val ext = path.substringAfterLast('.', "")
    return if (ext == CLASS_EXTENSION) {
      Element.Class(resolveClass(path), path)
    } else if (ext == RESOURCE_BUNDLE_EXTENSION) {
      Element.ResourceBundle(resolveBundleName(path))
    } else if (path.hasServiceProviders()) {
      Element.ServiceProvider(getServiceProvider(path), path)
    } else {
      Other
    }
  }

  private fun scan(zipEntry: ZipEntry) {
    when (val element = resolve(zipEntry)) {
      is Element.Class -> handle(element)
      is Element.ResourceBundle -> handle(element)
      is Element.ServiceProvider -> handle(element)
      Other -> Unit
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
    serviceProviderPaths += serviceProvider.path
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

  private fun resolve(path: String, separator: Char, suffix: String): String {
    return path.replace(File.separatorChar, separator)
      .removePrefix(separator.toString())
      .removeSuffix(suffix)
  }

  private fun resolveClass(path: PathInJar): String {
    return resolve(path, JAR_PATH_SEPARATOR_CHAR, CLASS_SUFFIX)
  }

  private fun resolveBundleName(path: PathInJar): String {
    return resolve(path, RESOURCE_BUNDLE_SEPARATOR, RESOURCE_BUNDLE_SUFFIX)
  }

  private fun String.normalizePath(): PathInJar {
    return replace(File.separatorChar, JAR_PATH_SEPARATOR_CHAR)
  }

  private fun PathInJar.hasServiceProviders(): Boolean {
    val path = replace(File.separatorChar, JAR_PATH_SEPARATOR_CHAR)
      .removePrefix(JAR_PATH_SEPARATOR_CHAR.toString())

    return path.startsWith("META-INF/services/") && count { it == JAR_PATH_SEPARATOR_CHAR } == 2
  }

  private fun getServiceProvider(path: PathInJar): String {
    return path.removePrefix("META-INF/services/")
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

  data class ClassInJar(val name: String, val path: PathInJar)

  sealed class Element {
    data class Class(val name: String, val path: PathInJar) : Element()
    data class ResourceBundle(val name: String) : Element()
    data class ServiceProvider(val name: String, val path: PathInJar) : Element()
    object Other : Element()
  }
}

