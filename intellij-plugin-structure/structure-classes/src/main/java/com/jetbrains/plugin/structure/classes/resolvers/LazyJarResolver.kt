package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.inputStream
import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult.NotFound
import com.jetbrains.plugin.structure.classes.resolvers.jar.Jar
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import com.jetbrains.plugin.structure.jar.SingletonCachingJarFileSystemProvider
import com.jetbrains.plugin.structure.jar.invoke
import org.objectweb.asm.tree.ClassNode
import java.nio.file.Path
import java.util.*

class LazyJarResolver(
  override val jarPath: Path,
  override val readMode: ReadMode,
  override val fileOrigin: FileOrigin,
  private val fileSystemProvider: JarFileSystemProvider = SingletonCachingJarFileSystemProvider
) : AbstractJarResolver(jarPath, readMode, fileOrigin), AutoCloseable  {

  private val jar: Jar by lazy {
    Jar(jarPath, fileSystemProvider).init()
  }

  override val bundleNames: MutableMap<String, MutableSet<String>>
    get() = jar.bundleNames.mapValues { it.value.toMutableSet() }.toMutableMap()

  override val allClasses: Set<String>
    get() = jar.classes

  override val allPackages: Set<String>
    get() = jar.packages

  override val allBundleNameSet: ResourceBundleNameSet
    get() = ResourceBundleNameSet(jar.bundleNames)

  override val implementedServiceProviders: Map<String, Set<String>>
    get() = jar.serviceProviders

  override fun resolveClass(className: String): ResolutionResult<ClassNode> {
    return jar.withClass(className) { className, classFilePath ->
      readClass(className, classFilePath)
    } ?: NotFound
  }

  override fun processAllClasses(processor: (ResolutionResult<ClassNode>) -> Boolean): Boolean {
    return jar.processAllClasses { className, classFilePath ->
      processor(readClass(className, classFilePath)) }
  }

  override fun containsClass(className: String): Boolean = jar.containsClass(className)

  override fun containsPackage(packageName: String): Boolean = jar.containsPackage(packageName)

  override fun close() = fileSystemProvider.close(jarPath)

  override fun readPropertyResourceBundle(bundleResourceName: String): PropertyResourceBundle? {
    return fileSystemProvider(jarPath) { fs ->
      val path = fs.getPath(bundleResourceName)
      if (path.exists()) {
        path.inputStream().use { PropertyResourceBundle(it) }
      } else {
        null
      }
    }
  }
}