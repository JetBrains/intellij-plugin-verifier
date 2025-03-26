package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.inputStream
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult.NotFound
import com.jetbrains.plugin.structure.classes.resolvers.jar.Jar
import com.jetbrains.plugin.structure.classes.utils.AsmUtil
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import org.objectweb.asm.tree.ClassNode
import java.nio.file.FileSystem
import java.nio.file.Path
import java.util.*

class LazyJarResolver(
  private val jarPath: Path,
  override val readMode: ReadMode,
  private val fileOrigin: FileOrigin,
  private val fileSystemProvider: JarFileSystemProvider
) : Resolver(), AutoCloseable  {

  private val jar: Jar by lazy {
    Jar(jarPath, fileSystemProvider).init()
  }

  private val bundleNames: Set<String>
    get() = jar.bundleNames.keys

  override val allClasses: Set<String>
    get() = jar.classes

  override val allPackages: Set<String>
    get() = jar.packages

  override val allBundleNameSet: ResourceBundleNameSet
    get() = ResourceBundleNameSet(jar.bundleNames)

  override fun resolveClass(className: String): ResolutionResult<ClassNode> {
    return jar.getClassInJar(className)
      ?.takeIf { it.path.exists() }
      ?.let { readClass(className, it.path) }
      ?: NotFound
  }

  override fun processAllClasses(processor: (ResolutionResult<ClassNode>) -> Boolean): Boolean {
    return jar.processAllClasses { (className, classFile) ->
      processor(readClass(className, classFile))
    }
  }

  override fun containsClass(className: String): Boolean = jar.containsClass(className)

  override fun containsPackage(packageName: String): Boolean = jar.containsPackage(packageName)

  override fun close() = fileSystemProvider.close(jarPath)

  //FIXME duplicate with JarFileResolver
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

  //FIXME optimize with JAR implementation
  private fun readPropertyResourceBundle(bundleResourceName: String): PropertyResourceBundle? {
    return useFileSystem { fs ->
      val path = fs.getPath(bundleResourceName)
      if (path.exists()) {
        path.inputStream().use { PropertyResourceBundle(it) }
      } else {
        null
      }
    }
  }

  //FIXME duplicate with JarFileResolver
  private fun readClass(className: String, classPath: Path): ResolutionResult<ClassNode> {
    return try {
      val classNode = classPath.inputStream().use {
        AsmUtil.readClassNode(className, it, readMode == ReadMode.FULL)
      }
      ResolutionResult.Found(classNode, fileOrigin)
    } catch (e: InvalidClassFileException) {
      ResolutionResult.Invalid(e.message)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      ResolutionResult.FailedToRead(e.message ?: e.javaClass.name)
    }
  }

  private fun <T> useFileSystem(useFileSystem: (FileSystem) -> T): T {
    return fileSystemProvider.getFileSystem(jarPath).use {
      useFileSystem(it)
    }
  }
}