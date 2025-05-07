package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.base.BinaryClassName
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.classes.utils.AsmUtil
import com.jetbrains.plugin.structure.jar.Jar
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import com.jetbrains.plugin.structure.jar.PathInJar
import com.jetbrains.plugin.structure.jar.SingletonCachingJarFileSystemProvider
import com.jetbrains.plugin.structure.jar.newZipHandler
import org.objectweb.asm.tree.ClassNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.*

private val LOG: Logger = LoggerFactory.getLogger(LazyJarResolver::class.java)

class LazyJarResolver(
  public override val jarPath: Path,
  override val readMode: ReadMode,
  override val fileOrigin: FileOrigin,
  override val name: String = jarPath.fileName.toString(),
  private val fileSystemProvider: JarFileSystemProvider = SingletonCachingJarFileSystemProvider
) : AbstractJarResolver(jarPath, readMode, fileOrigin), AutoCloseable  {

  private val jar: Jar by lazy {
    Jar(jarPath, fileSystemProvider).init()
  }

  private val zipHandler = jarPath.newZipHandler()

  override val bundleNames: MutableMap<String, MutableSet<String>>
    get() = jar.bundleNames.mapValues { it.value.toMutableSet() }.toMutableMap()

  @Deprecated("Use 'allClassNames' property instead which is more efficient")
  override val allClasses: Set<String> by lazy  {
    jar.classes.mapTo(hashSetOf()) { it.toString() }
  }

  override val allClassNames: Set<BinaryClassName> by lazy {
    jar.classes
  }

  @Deprecated("Use 'packages' property instead. This property may be slow on some file systems.")
  override val allPackages: Set<String> by lazy { jar.packages.all }

  override val packages: Set<String> by lazy { jar.packages.entries }

  override val allBundleNameSet: ResourceBundleNameSet by lazy {
    ResourceBundleNameSet(jar.bundleNames)
  }

  override val implementedServiceProviders: Map<String, Set<String>> by lazy {
    jar.serviceProviders
  }

  @Deprecated("Use 'resolveClass(BinaryClassName)' instead")
  override fun resolveClass(className: String): ResolutionResult<ClassNode> {
    return jar.processClassPathInJar(className) { className, classFilePath ->
      readClass(className, classFilePath)
    } ?: ResolutionResult.NotFound
  }

  override fun resolveClass(className: BinaryClassName): ResolutionResult<ClassNode> {
    return resolveClass(className.toString())
  }

  override fun processAllClasses(processor: (ResolutionResult<ClassNode>) -> Boolean): Boolean {
    return jar.processAllClasses { className, classFilePath ->
      processor(readClass(className, classFilePath)) }
  }

  @Deprecated("Use 'containsClass(BinaryClassName)' instead")
  override fun containsClass(className: String): Boolean = jar.containsClass(className)

  override fun containsClass(className: BinaryClassName) = containsClass(className.toString())

  override fun containsPackage(packageName: String): Boolean = jar.containsPackage(packageName)

  override fun close() = Unit

  fun readClass(className: CharSequence, classPath: PathInJar): ResolutionResult<ClassNode> {
    return try {
      zipHandler.handleEntry(classPath) { entryResource, entry ->
        val inputStream = entryResource.getInputStream(entry)
        val classNode = AsmUtil.readClassNode(className, inputStream, readMode == ReadMode.FULL)
        ResolutionResult.Found(classNode, fileOrigin)
      } ?: ResolutionResult.NotFound
    } catch (e: InvalidClassFileException) {
      ResolutionResult.Invalid(e.message)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      ResolutionResult.FailedToRead(e.message ?: e.javaClass.name)
    }
  }

  override fun readPropertyResourceBundle(bundleResourceName: String): PropertyResourceBundle? {
    return zipHandler.handleEntry(bundleResourceName) { bundleResourceName, entry ->
      val inputStream = bundleResourceName.getInputStream(entry)
      PropertyResourceBundle(inputStream)
    }
  }
}