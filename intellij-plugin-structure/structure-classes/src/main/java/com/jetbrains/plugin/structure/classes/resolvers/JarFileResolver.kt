package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.base.utils.closeOnException
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.classes.utils.AsmUtil
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile

class JarFileResolver(
    private val ioJarFile: Path,
    override val readMode: ReadMode,
    private val classFileOrigin: ClassFileOrigin
) : Resolver() {

  private companion object {
    private const val CLASS_SUFFIX = ".class"

    private const val SERVICE_PROVIDERS_PREFIX = "META-INF/services/"
  }

  private val jarFile: JarFile

  private val classes: MutableSet<String> = hashSetOf()

  private val packageSet = PackageSet()

  private val serviceProviders: MutableMap<String, Set<String>> = hashMapOf()

  init {
    if (!Files.exists(ioJarFile)) {
      throw IllegalArgumentException("Jar file $ioJarFile doesn't exist")
    }
    if (!ioJarFile.fileName.toString().endsWith(".jar")) {
      throw IllegalArgumentException("File $ioJarFile is not a jar archive")
    }

    jarFile = JarFile(ioJarFile.toFile())
    try {
      readClassNamesAndServiceProviders()
    } catch (e: Throwable) {
      jarFile.closeLogged()
      throw e
    }
  }

  private fun readClassNamesAndServiceProviders() {
    for (entry in jarFile.entries().iterator()) {
      val entryName = entry.name
      if (entryName.endsWith(CLASS_SUFFIX)) {
        val className = entryName.substringBeforeLast(CLASS_SUFFIX)
        classes.add(className)
        packageSet.addPackagesOfClass(className)
      } else if (!entry.isDirectory && entryName.startsWith(SERVICE_PROVIDERS_PREFIX) && entryName.count { it == '/' } == 2) {
        val serviceProvider = entryName.substringAfter(SERVICE_PROVIDERS_PREFIX)
        serviceProviders[serviceProvider] = readServiceImplementationNames(serviceProvider)
      }
    }
  }

  private fun readServiceImplementationNames(serviceProvider: String): Set<String> {
    val entry = SERVICE_PROVIDERS_PREFIX + serviceProvider
    val jarEntry = jarFile.getJarEntry(entry) ?: return emptySet()
    val lines = jarFile.getInputStream(jarEntry).reader().readLines()
    return lines.map { it.substringBefore("#").trim() }.filterNotTo(hashSetOf()) { it.isEmpty() }
  }

  val implementedServiceProviders: Map<String, Set<String>> = serviceProviders

  override val allPackages: Set<String>
    get() = packageSet.getAllPackages()

  override val allClasses
    get() = classes

  override val isEmpty
    get() = classes.isEmpty()

  override fun processAllClasses(processor: (ClassNode) -> Boolean): Boolean {
    for (jarEntry in jarFile.entries().iterator()) {
      val entryName = jarEntry.name
      if (entryName.endsWith(CLASS_SUFFIX)) {
        val className = entryName.substringBeforeLast(CLASS_SUFFIX)
        jarFile.getInputStream(jarEntry).use { entryInputStream ->
          val classNode = AsmUtil.readClassNode(className, entryInputStream, readMode == ReadMode.FULL)
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

  override fun resolveClass(className: String): ResolutionResult {
    if (className !in classes) {
      return ResolutionResult.NotFound
    }
    val classNode = try {
      evaluateNode(className)
    } catch (e: InvalidClassFileException) {
      return ResolutionResult.InvalidClassFile(e.message)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      return ResolutionResult.FailedToReadClassFile(e.localizedMessage ?: e.javaClass.name)
    } ?: return ResolutionResult.NotFound
    return ResolutionResult.Found(classNode, classFileOrigin)
  }

  private fun evaluateNode(className: String): ClassNode? {
    val entry = jarFile.getEntry(className + CLASS_SUFFIX) ?: return null
    jarFile.getInputStream(entry).use { inputStream ->
      return AsmUtil.readClassNode(className, inputStream, readMode == ReadMode.FULL)
    }
  }

  override fun close() = jarFile.close()

  override fun toString() = ioJarFile.toAbsolutePath().toString()

}

fun buildJarFileResolvers(
    jars: Iterable<File>,
    readMode: Resolver.ReadMode,
    parentOrigin: ClassFileOrigin
): List<Resolver> {
  val resolvers = arrayListOf<Resolver>()
  resolvers.closeOnException {
    jars.mapTo(resolvers) { jarFile ->
      val jarClassFileOrigin = JarClassFileOrigin(jarFile.name, parentOrigin)
      JarFileResolver(jarFile.toPath(), readMode, jarClassFileOrigin)
    }
  }
  return resolvers
}