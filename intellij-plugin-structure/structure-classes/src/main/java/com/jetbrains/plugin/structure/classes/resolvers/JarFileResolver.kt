package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.base.utils.FileUtil
import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.classes.utils.AsmUtil
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.util.jar.JarFile

class JarFileResolver(jar: File) : Resolver() {

  private companion object {
    private const val CLASS_SUFFIX = ".class"

    private const val SERVICE_PROVIDERS_PREFIX = "META-INF/services/"
  }

  private val ioJarFile: File

  private val jarFile: JarFile

  private val classes: MutableSet<String> = hashSetOf()

  private val serviceProviders: MutableSet<String> = hashSetOf()

  init {
    if (!jar.exists()) {
      throw IllegalArgumentException("Jar file $jar doesn't exist")
    }
    if (!FileUtil.isJar(jar)) {
      throw IllegalArgumentException("File $jar is not a jar archive")
    }
    ioJarFile = jar

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
    //todo: speedup.
    return allClasses.asSequence()
        .mapNotNull { findClass(it) }
        .all(processor)
  }

  override fun containsClass(className: String): Boolean = classes.contains(className)

  override fun findClass(className: String): ClassNode? =
      if (className in classes) evaluateNode(className) else null

  private fun evaluateNode(className: String): ClassNode? {
    val entry = jarFile.getEntry(className + CLASS_SUFFIX) ?: return null
    jarFile.getInputStream(entry).use { inputStream ->
      return AsmUtil.readClassNode(className, inputStream)
    }
  }

  override fun getClassLocation(className: String): Resolver? = if (containsClass(className)) this else null

  override fun close() {
    jarFile.close()
  }

}
