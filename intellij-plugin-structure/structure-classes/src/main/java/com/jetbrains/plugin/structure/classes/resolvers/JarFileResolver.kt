package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.base.utils.*
import com.jetbrains.plugin.structure.classes.utils.AsmUtil
import com.jetbrains.plugin.structure.classes.utils.getBundleBaseName
import com.jetbrains.plugin.structure.classes.utils.getBundleNameByBundlePath
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.jar.JarFile

class JarFileResolver(
  private val ioJarFile: Path,
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

  private val jarFile: JarFile

  init {
    require(ioJarFile.exists()) { "Jar file $ioJarFile doesn't exist" }
    require(ioJarFile.simpleName.endsWith(".jar")) { "File $ioJarFile is not a jar archive" }
    jarFile = JarFile(ioJarFile.toFile())
    readClassNamesAndServiceProviders()
  }

  private fun readClassNamesAndServiceProviders() {
    for (entry in jarFile.entries().iterator()) {
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
        serviceProviders[serviceProvider] = readServiceImplementationNames(serviceProvider, jarFile)
      }
    }
  }

  private fun readServiceImplementationNames(serviceProvider: String, jarFile: JarFile): Set<String> {
    val entry = SERVICE_PROVIDERS_PREFIX + serviceProvider
    val jarEntry = jarFile.getJarEntry(entry) ?: return emptySet()
    val lines = jarFile.getInputStream(jarEntry).reader().readLines()
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
    for (jarEntry in jarFile.entries().iterator()) {
      val entryName = jarEntry.name
      if (entryName.endsWith(CLASS_SUFFIX)) {
        val className = entryName.substringBeforeLast(CLASS_SUFFIX)
        jarFile.getInputStream(jarEntry).use {
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
    val resourceEntry = jarFile.getEntry(bundleResourceName) ?: return null
    return jarFile.getInputStream(resourceEntry).use {
      PropertyResourceBundle(it)
    }
  }

  private fun readClassNode(className: String): ClassNode? {
    val entry = jarFile.getEntry(className + CLASS_SUFFIX) ?: return null
    return jarFile.getInputStream(entry).use {
      AsmUtil.readClassNode(className, it, readMode == ReadMode.FULL)
    }
  }

  override fun close() = jarFile.close()

  override fun toString() = ioJarFile.toAbsolutePath().toString()

}

fun buildJarFileResolvers(
  jars: Iterable<File>,
  readMode: Resolver.ReadMode,
  parentOrigin: FileOrigin
): List<Resolver> {
  val resolvers = arrayListOf<Resolver>()
  resolvers.closeOnException {
    jars.mapTo(resolvers) { jarFile ->
      val jarFileOrigin = JarFileOrigin(jarFile.name, parentOrigin)
      JarFileResolver(jarFile.toPath(), readMode, jarFileOrigin)
    }
  }
  return resolvers
}