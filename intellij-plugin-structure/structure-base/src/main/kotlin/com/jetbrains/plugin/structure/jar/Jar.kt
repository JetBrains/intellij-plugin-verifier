package com.jetbrains.plugin.structure.jar

import com.jetbrains.plugin.structure.base.utils.CharSequenceComparator
import com.jetbrains.plugin.structure.base.utils.charseq.CharBufferCharSequence
import com.jetbrains.plugin.structure.base.utils.charseq.CharReplacingCharSequence
import com.jetbrains.plugin.structure.base.utils.getBundleBaseName
import com.jetbrains.plugin.structure.base.utils.isFile
import com.jetbrains.plugin.structure.jar.Jar.DescriptorType.*
import com.jetbrains.plugin.structure.jar.JarEntryResolver.Key
import com.jetbrains.plugin.structure.jar.descriptors.Descriptor
import com.jetbrains.plugin.structure.jar.descriptors.ModuleDescriptorReference
import com.jetbrains.plugin.structure.jar.descriptors.PluginDescriptorReference
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.CharBuffer
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import kotlin.streams.asSequence


private val LOG: Logger = LoggerFactory.getLogger(Jar::class.java)

private val NO_SUFFIX: CharBuffer = CharBuffer.allocate(0)

private val CLASS_SUFFIX: CharBuffer = CharBuffer.wrap(".class")

private val RESOURCE_BUNDLE_EXTENSION: CharBuffer = CharBuffer.wrap("properties")
private val RESOURCE_BUNDLE_SUFFIX: CharBuffer = CharBuffer.wrap(".properties")

private val XML_DESCRIPTOR_SUFFIX: CharBuffer = CharBuffer.wrap(".xml")

private const val JAR_PATH_SEPARATOR_CHAR = '/'

private const val RESOURCE_BUNDLE_SEPARATOR = '.'

typealias PathInJar = CharSequence

class Jar(
  val jarPath: Path,
  private val fileSystemProvider: JarFileSystemProvider,
  private val entryResolvers: List<JarEntryResolver<*>> = emptyList()
) : AutoCloseable {

  private val classesInJar = TreeMap<CharSequence, PathInJar>(CharSequenceComparator)

  val classes: Set<CharSequence> = classesInJar.keys

  val packages: Packages by lazy {
    Packages().apply {
      classes.forEach {
        addClass(it)
      }
    }
  }

  private val _bundleNames = mutableMapOf<CharSequence, MutableSet<String>>()
  val bundleNames: Map<String, Set<String>> get() = _bundleNames.mapKeys { (k, _) -> k.toString() }

  val serviceProviders: Map<String, Set<String>> by lazy {
    val serviceProviders = mutableMapOf<String, MutableSet<String>>()
    fileSystemProvider.getFileSystem(jarPath).use { fs ->
      serviceProviderPaths.forEach { spPath ->
        val serviceProviderFile = fs.getPath(spPath.toString())
        if (serviceProviderFile.isFile) {
          val serviceProviderNames = readServiceImplementationNames(serviceProviderFile)
          serviceProviders.getOrPut(serviceProviderFile.fileName.toString()) { mutableSetOf() } += serviceProviderNames
        }
      }
    }
    serviceProviders
  }

  private val serviceProviderPaths = mutableSetOf<CharSequence>()

  val descriptorCandidates = mutableSetOf<Descriptor>()

  val entryResolverResults: MutableMap<Key<*>, MutableList<Any?>> = mutableMapOf()

  @Throws(JarArchiveException::class)
  fun init(): Jar = apply {
    if (jarPath.supportsFile()) {
      init(ZipResource.ZipFile(jarPath))
    } else {
      init(ZipResource.ZipPath(jarPath))
    }
  }

  fun processAllClasses(processor: (String, Path) -> Boolean): Boolean {
    return getFileSystem(jarPath, classesInJar.size).use { fs ->
      classesInJar.all { (className, classFilePath) ->
        getFileSystem(jarPath, expectedClients = 1).use { classFs ->
          classFs.getPath(classFilePath.toString())
            .takeIf { it.isFile }
            ?.let { processor(className.toString(), it) } == true
        }
      }
    }
  }

  fun containsPackage(packageName: String) = packages.contains(packageName)

  fun containsClass(className: String) = className in classes

  private fun getPath(className: String): PathInJar? = classesInJar[className]

  private fun getFileSystem(jarPath: Path, expectedClients: Int): FileSystem {
    return fileSystemProvider.getFileSystem(jarPath, JarFileSystemProvider.Configuration(expectedClients))
  }

  fun <T> processClassPathInJar(className: String, handler: (String, PathInJar) -> T): T? {
    return getPath(className)?.let { pathInJar ->
      handler(className, pathInJar)
    }
  }

  override fun close() {
    // NO-OP
  }

  private fun Path.supportsFile() = fileSystem == FileSystems.getDefault()

  @Throws(JarArchiveException::class)
  private fun init(zipFile: ZipResource.ZipFile) = try {
    ZipFile(zipFile.file).use { zip ->
      zip.entries().asIterator().forEach {
        if (!it.isDirectory) {
          scan(it)
        }
      }
    }
  } catch (e: IOException) {
    throw JarArchiveException("JAR archive could not be opened at [$jarPath]: ${e.message} ", e)
  }

  private fun init(zipPath: ZipResource.ZipPath) {
    Files.newInputStream(zipPath.path).use { it ->
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

  private fun scan(zipEntry: ZipEntry) {
    val path = PathWithinJar.of(zipEntry)
    if (path.isClass()) {
      handleClass(resolveClass(path), path.path)
    } else if (path.isResourceBundle()) {
      handleResourceBundle(resolveBundleName(path))
    } else if (path.hasServiceProviders()) {
      handleServiceProvider(path.path)
    } else {
      val descriptorType = path.matchesDescriptor()
      if (descriptorType != NO_MATCH) {
        handleDescriptorCandidate(zipEntry, path, descriptorType)
      } else {
        entryResolvers.forEach { resolver ->
          resolver.resolve(path.path, zipEntry)?.let {
            entryResolverResults.getOrPut(resolver.key) { mutableListOf() } += it
          }
        }
      }
    }
  }

  private fun handleClass(className: CharSequence, classPath: PathInJar) {
    classesInJar[className] = classPath
  }

  private fun handleResourceBundle(resourceBundleName: CharSequence) {
    val fullBundleName = resourceBundleName.toString()
    _bundleNames.getOrPut(getBundleBaseName(fullBundleName)) { mutableSetOf() } += fullBundleName
  }

  private fun handleServiceProvider(serviceProviderPath: PathInJar) {
    serviceProviderPaths += serviceProviderPath
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

  private fun resolve(path: PathWithinJar, separator: Char, suffix: CharSequence): CharSequence {
    val pathBuf = path.path
    val noPrefix = if (pathBuf.get(0) == separator) {
      pathBuf.subSequence(1, pathBuf.length)
    } else {
      pathBuf
    }
    val neitherPrefixNoSuffix = if (suffix.isNotEmpty() && noPrefix.endsWith(suffix)) {
      CharBufferCharSequence(noPrefix, 0, noPrefix.length - suffix.length)
    } else {
      noPrefix
    }
    return if (separator == File.separatorChar) {
      neitherPrefixNoSuffix
    } else {
      CharReplacingCharSequence(neitherPrefixNoSuffix, File.separatorChar, separator)
    }
  }

  private fun resolveClass(path: PathWithinJar): CharSequence {
    if (File.separatorChar == JAR_PATH_SEPARATOR_CHAR) {
      return path.removeSuffix(CLASS_SUFFIX)
    }

    return resolve(path, JAR_PATH_SEPARATOR_CHAR, CLASS_SUFFIX)
  }

  private fun resolveBundleName(path: PathWithinJar): CharSequence {
    return resolve(path, RESOURCE_BUNDLE_SEPARATOR, RESOURCE_BUNDLE_SUFFIX)
  }

  private fun handleDescriptorCandidate(zipEntry: ZipEntry, path: PathWithinJar, descriptorType: DescriptorType) {
    when (descriptorType) {
      PLUGIN -> descriptorCandidates += PluginDescriptorReference(jarPath, path.path)
      MODULE -> descriptorCandidates += ModuleDescriptorReference(jarPath, path.path)
      else -> Unit
    }
  }

  private fun PathWithinJar.hasServiceProviders(): Boolean {
    val spPath = resolve(this, JAR_PATH_SEPARATOR_CHAR, NO_SUFFIX)
    return spPath.startsWith("META-INF/services/") && spPath.occurrences(JAR_PATH_SEPARATOR_CHAR) == 2
  }

  private fun PathWithinJar.matchesDescriptor(): DescriptorType {
    if (!isXml()) return NO_MATCH
    val descriptorPath = resolve(this, JAR_PATH_SEPARATOR_CHAR, NO_SUFFIX)
    if (descriptorPath.startsWith("META-INF/") && descriptorPath.occurrences(JAR_PATH_SEPARATOR_CHAR) == 1) return PLUGIN
    if (descriptorPath.occurrences(JAR_PATH_SEPARATOR_CHAR) == 0) return MODULE
    return NO_MATCH
  }

  private fun CharSequence.occurrences(c: Char): Int {
    var count = 0
    for (i in 0..length - 1) {
      if (this[i] == c) {
        count++
      }
    }
    return count
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

  private data class PathWithinJar(val path: CharBuffer) {
    companion object {
      fun of(zipEntry: ZipEntry) = PathWithinJar(CharBuffer.wrap(zipEntry.name))
    }

    fun isClass(): Boolean = path.endsWith(CLASS_SUFFIX)
    fun isResourceBundle(): Boolean = path.endsWith(RESOURCE_BUNDLE_EXTENSION)
    fun isXml(): Boolean = path.endsWith(XML_DESCRIPTOR_SUFFIX)

    fun removePrefix(prefix: String): CharSequence {
      if (!path.startsWith(prefix)) return path
      return path.subSequence(0, prefix.length)
    }

    fun removeSuffix(suffix: CharSequence): CharSequence {
      if (!path.endsWith(suffix)) return path
      return CharBufferCharSequence(path, 0, path.length - suffix.length)
    }

    override fun toString(): String = path.toString()
  }

  sealed class ZipResource {
    data class ZipFile(val path: Path) : ZipResource() {
      val file: File = path.toFile()
    }

    data class ZipPath(val path: Path) : ZipResource()
  }

  enum class DescriptorType {
    NO_MATCH, PLUGIN, MODULE
  }
}

