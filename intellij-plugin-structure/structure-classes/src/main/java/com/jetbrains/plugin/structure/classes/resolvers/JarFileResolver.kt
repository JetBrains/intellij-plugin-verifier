package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.base.utils.FileUtil
import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.classes.utils.AsmUtil
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.util.jar.JarFile

class JarFileResolver(private val ioJarFile: File) : Resolver() {

  private companion object {
    private const val CLASS_SUFFIX = ".class"

    private const val SERVICE_PROVIDERS_PREFIX = "META-INF/services/"
  }

  private val jarFile: JarFile

  private val classes: MutableSet<String> = hashSetOf()

  private val serviceProviders: MutableSet<String> = hashSetOf()

  init {
    if (!ioJarFile.exists()) {
      throw IllegalArgumentException("Jar file $ioJarFile doesn't exist")
    }
    if (!FileUtil.isJar(ioJarFile)) {
      throw IllegalArgumentException("File $ioJarFile is not a jar archive")
    }

    jarFile = JarFile(ioJarFile)
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
        classes.add(entryName.substringBeforeLast(CLASS_SUFFIX))
      } else if (!entry.isDirectory && entryName.startsWith(SERVICE_PROVIDERS_PREFIX) && entryName.count { it == '/' } == 2) {
        serviceProviders.add(entryName.substringAfter(SERVICE_PROVIDERS_PREFIX))
      }
    }
  }

  fun readServiceImplementationNames(serviceProvider: String): Set<String> {
    val entry = SERVICE_PROVIDERS_PREFIX + serviceProvider
    val jarEntry = jarFile.getJarEntry(entry) ?: return emptySet()
    val lines = jarFile.getInputStream(jarEntry).reader().readLines()
    return lines.map { it.substringBefore("#").trim() }.filterNotTo(hashSetOf()) { it.isEmpty() }
  }

  val implementedServiceProviders: Set<String> = serviceProviders

  override val allClasses
    get() = classes

  override val isEmpty
    get() = classes.isEmpty()

  override val classPath
    get() = listOf(ioJarFile)

  override val finalResolvers
    get() = listOf(this)

  override fun processAllClasses(processor: (ClassNode) -> Boolean): Boolean {
    for (jarEntry in jarFile.entries().iterator()) {
      val entryName = jarEntry.name
      if (entryName.endsWith(CLASS_SUFFIX)) {
        val className = entryName.substringBeforeLast(CLASS_SUFFIX)
        jarFile.getInputStream(jarEntry).use { entryInputStream ->
          val classNode = AsmUtil.readClassNode(className, entryInputStream)
          if (!processor(classNode)) {
            return false
          }
        }
      }
    }
    return true
  }

  override fun containsClass(className: String) = className in classes

  override fun findClass(className: String) =
      if (className in classes) evaluateNode(className) else null

  private fun evaluateNode(className: String): ClassNode? {
    val entry = jarFile.getEntry(className + CLASS_SUFFIX) ?: return null
    jarFile.getInputStream(entry).use { inputStream ->
      return AsmUtil.readClassNode(className, inputStream)
    }
  }

  override fun getClassLocation(className: String) = if (containsClass(className)) this else null

  override fun close() = jarFile.close()

  override fun toString() = ioJarFile.canonicalPath!!

}
