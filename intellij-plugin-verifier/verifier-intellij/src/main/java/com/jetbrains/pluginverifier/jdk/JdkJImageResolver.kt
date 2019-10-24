package com.jetbrains.pluginverifier.jdk

import com.jetbrains.plugin.structure.base.utils.closeAll
import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.classes.resolvers.*
import com.jetbrains.plugin.structure.classes.utils.AsmUtil
import org.objectweb.asm.tree.ClassNode
import java.io.Closeable
import java.net.URI
import java.net.URLClassLoader
import java.nio.file.*
import java.util.*
import java.util.stream.Collectors

/**
 * [Resolver] that reads class files from JImage corresponding to `<JDK>/lib/modules` file of JDK 9 and later.
 */
class JdkJImageResolver(jdkPath: Path, override val readMode: ReadMode) : Resolver() {
  private companion object {

    val JRT_SCHEME_URI: URI = URI.create("jrt:/")
  }

  private val fileOrigin: FileOrigin = JdkFileOrigin(jdkPath)

  private val classNameToModuleName: Map<String, String>

  private val packageSet = PackageSet()

  private val nameSeparator: String

  private val modulesPath: Path

  private val closeableResources = arrayListOf<Closeable>()

  init {
    val fileSystem = try {
      getOrCreateJrtFileSystem(jdkPath)
    } catch (e: Exception) {
      throw RuntimeException("Unable to read content from jrt:/ file system.", e)
    }

    nameSeparator = fileSystem.separator

    modulesPath = fileSystem.getPath("/modules")

    classNameToModuleName = Files.walk(modulesPath)
      .filter { p -> p.fileName.toString().endsWith(".class") }
      .collect(
        Collectors.toMap(
          { p -> getClassName(p) },
          { p -> getModuleName(p) },
          { one, _ -> one }
        )
      )

    for (className in classNameToModuleName.keys) {
      packageSet.addPackagesOfClass(className)
    }
  }

  private fun getOrCreateJrtFileSystem(jdkPath: Path): FileSystem {
    try {
      return FileSystems.getFileSystem(JRT_SCHEME_URI)
    } catch (e: Exception) {
      val jrtJar = jdkPath.resolve("lib").resolve("jrt-fs.jar")
      require(jrtJar.exists()) { "Invalid JDK: $jrtJar does not exist" }

      val classLoader = URLClassLoader(arrayOf(jrtJar.toUri().toURL()))
      return try {
        val fileSystem = FileSystems.newFileSystem(JRT_SCHEME_URI, hashMapOf<String, Any>(), classLoader)
        closeableResources += classLoader
        fileSystem
      } catch (e: FileSystemAlreadyExistsException) {
        classLoader.closeLogged()

        //File system might be already created concurrently. Try to get existing file system again.
        FileSystems.getFileSystem(JRT_SCHEME_URI)
      }
    }
  }

  private fun getModuleName(classPath: Path): String =
    modulesPath.relativize(classPath).first().toString()

  private fun getClassName(classPath: Path): String {
    val relative = modulesPath.relativize(classPath)
    return relative
      .subpath(1, relative.nameCount).toString()
      .substringBeforeLast(".class").replace(nameSeparator, "/")
  }

  override val allClasses
    get() = classNameToModuleName.keys

  override val allPackages
    get() = packageSet.getAllPackages()

  override val allBundleNameSet
    get() = ResourceBundleNameSet(emptyMap())

  override fun resolveClass(className: String): ResolutionResult<ClassNode> {
    val moduleName = classNameToModuleName[className]
    if (moduleName != null) {
      val classPath = modulesPath.resolve(moduleName).resolve(className.replace("/", nameSeparator) + ".class")
      val classNode = try {
        readClassNode(className, classPath)
      } catch (e: InvalidClassFileException) {
        return ResolutionResult.Invalid(e.message)
      } catch (e: Exception) {
        e.rethrowIfInterrupted()
        return ResolutionResult.FailedToRead(e.localizedMessage ?: e.javaClass.name)
      }
      return ResolutionResult.Found(classNode, fileOrigin)
    }
    return ResolutionResult.NotFound
  }

  override fun resolveExactPropertyResourceBundle(baseName: String, locale: Locale) = ResolutionResult.NotFound

  private fun readClassNode(className: String, classFilePath: Path): ClassNode =
    Files.newInputStream(classFilePath, StandardOpenOption.READ).use { inputStream ->
      AsmUtil.readClassNode(className, inputStream, readMode == ReadMode.FULL)
    }

  override fun containsClass(className: String) = className in classNameToModuleName

  override fun containsPackage(packageName: String) = packageSet.containsPackage(packageName)

  override fun processAllClasses(processor: (ClassNode) -> Boolean): Boolean {
    for (classPath in Files.walk(modulesPath).filter { it.fileName.toString().endsWith(".class") }) {
      val className = getClassName(classPath)
      val classNode = readClassNode(className, classPath)
      if (!processor(classNode)) {
        return false
      }
    }
    return true
  }

  override fun close() {
    closeableResources.closeAll()
  }
}