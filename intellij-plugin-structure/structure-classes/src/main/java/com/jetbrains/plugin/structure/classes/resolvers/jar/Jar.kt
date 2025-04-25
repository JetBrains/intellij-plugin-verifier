package com.jetbrains.plugin.structure.classes.resolvers.jar

import com.jetbrains.plugin.structure.base.utils.isFile
import com.jetbrains.plugin.structure.classes.resolvers.Packages
import com.jetbrains.plugin.structure.classes.utils.getBundleBaseName
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import com.jetbrains.plugin.structure.jar.invoke
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.CharBuffer
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import kotlin.math.min
import kotlin.streams.asSequence

private val LOG: Logger = LoggerFactory.getLogger(Jar::class.java)

private val NO_SUFFIX: CharBuffer = CharBuffer.allocate(0)

private val CLASS_SUFFIX: CharBuffer = CharBuffer.wrap(".class")

private val RESOURCE_BUNDLE_EXTENSION: CharBuffer = CharBuffer.wrap("properties")
private val RESOURCE_BUNDLE_SUFFIX: CharBuffer = CharBuffer.wrap(".properties")

private const val JAR_PATH_SEPARATOR_CHAR = '/'

private const val RESOURCE_BUNDLE_SEPARATOR = '.'

typealias PathInJar = CharSequence

class Jar(
  val jarPath: Path,
  private val fileSystemProvider: JarFileSystemProvider
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
    useFileSystem { fs ->
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

  fun init(): Jar = apply {
    if (jarPath.supportsFile()) {
      init(jarPath.toFile())
    } else {
      init(jarPath)
    }
  }

  fun processAllClasses(processor: (String, Path) -> Boolean): Boolean {
    return useFileSystem { fs ->
      classesInJar.all { (className, classFilePath) ->
        fs.getPath(classFilePath.toString())
          .takeIf { it.isFile }
          ?.let { processor(className.toString(), it) } == true
      }
    }
  }

  private fun <T> useFileSystem(action: (FileSystem) -> T): T {
    return fileSystemProvider(jarPath, action)
  }

  fun containsPackage(packageName: String) = packages.contains(packageName)

  fun containsClass(className: String) = className in classes

  private fun getPath(className: String): PathInJar? = classesInJar[className]

  fun <T> withClass(className: String, handler: (String, Path) -> T): T? {
    return getPath(className)?.let { pathInJar ->
      useFileSystem {
        it.getPath(pathInJar.toString())
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

  private fun scan(zipEntry: ZipEntry) {
    val path = PathWithinJar.of(zipEntry)
    if (path.isClass()) {
      handleClass(resolveClass(path), path.path)
    } else if (path.isResourceBundle()) {
      handleResourceBundle(resolveBundleName(path))
    } else if (path.hasServiceProviders()) {
      handleServiceProvider(path.path)
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
      noPrefix.subSequence(0, noPrefix.length - suffix.length)
    } else {
      noPrefix
    }
    return if (separator == File.separatorChar) {
      neitherPrefixNoSuffix
    } else {
      CharReplacer(neitherPrefixNoSuffix, File.separatorChar, separator)
    }
  }

  private fun resolveClass(path: PathWithinJar): CharSequence {
    return resolve(path, JAR_PATH_SEPARATOR_CHAR, CLASS_SUFFIX)
  }

  private fun resolveBundleName(path: PathWithinJar): CharSequence {
    return resolve(path, RESOURCE_BUNDLE_SEPARATOR, RESOURCE_BUNDLE_SUFFIX)
  }

  private fun PathWithinJar.hasServiceProviders(): Boolean {
    val spPath = resolve(this, JAR_PATH_SEPARATOR_CHAR, NO_SUFFIX)
    return spPath.startsWith("META-INF/services/") && spPath.occurrences(JAR_PATH_SEPARATOR_CHAR) == 2
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

    fun removePrefix(prefix: String): CharSequence {
      if (!path.startsWith(prefix)) return path
      return path.subSequence(0, prefix.length)
    }

    override fun toString(): String = path.toString()
  }

  class CharReplacer(private val buf: CharBuffer, private val oldChar: Char, private val replacement: Char) :
    CharSequence {
    override val length: Int
      get() = buf.length

    override fun get(index: Int): Char {
      val c = buf[index]
      return if (c == oldChar) replacement else c
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
      return CharReplacer(buf.subSequence(startIndex, endIndex), oldChar, replacement)
    }

    override fun toString(): String {
      val newBuf = CharBuffer.allocate(buf.length)
      for (i in 0..buf.length - 1) {
        newBuf.put(i, get(i))
      }
      return newBuf.toString()
    }
  }

  private object CharSequenceComparator : Comparator<CharSequence> {
    override fun compare(cs1: CharSequence, cs2: CharSequence): Int {
      if (cs1 === cs2) return 0

      val len1 = cs1.length
      val len2 = cs2.length
      val shorterLen = min(len1, len2)

      for (i in 0..shorterLen - 1) {
        val c1 = cs1[i]
        val c2 = cs2[i]
        if (c1 != c2) {
          return c1.compareTo(c2)
        }
      }
      return len1 - len2
    }
  }
}

