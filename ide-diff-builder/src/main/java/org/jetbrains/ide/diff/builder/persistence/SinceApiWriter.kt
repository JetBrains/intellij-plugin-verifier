package org.jetbrains.ide.diff.builder.persistence

import com.jetbrains.plugin.structure.base.utils.archiveDirectory
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.*
import org.jetbrains.ide.diff.builder.api.SinceApiData
import org.jetbrains.ide.diff.builder.signatures.ApiSignature
import java.io.Closeable
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*

/**
 * Utility class used to save [SinceApiData] to external annotations roots.
 *
 * Creates necessary package-like directory structure, for example
 * ```
 * org/
 * org/some/
 * org/some/annotations.xml
 * org/some/util/
 * org/some/util/annotations.xml
 * ```
 *
 * This class is not thread safe.
 */
class SinceApiWriter(private val annotationsRoot: Path) : Closeable {

  private companion object {
    /**
     * Maximum number of files opened for writing at the same time.
     * Limiting is necessary to avoid IO exception: "Too many open files"
     */
    const val MAX_OPEN_FILES = 50
  }

  init {
    require(annotationsRoot.extension == "" || annotationsRoot.extension == "zip") {
      "Only directory or .zip roots are supported"
    }
  }

  /**
   * All packages encountered during save.
   */
  private val allPackages = mutableSetOf<String>()

  /**
   * Maps package name to [Writer] used to write signatures of that package.
   * This map is limited in size with [MAX_OPEN_FILES].
   * When more writers get open, the eldest one is closed.
   */
  private val packageWriters = object : LinkedHashMap<String, SinceApiXmlWriter>() {
    override fun removeEldestEntry(eldest: Map.Entry<String, SinceApiXmlWriter>): Boolean {
      if (size > MAX_OPEN_FILES) {
        val xmlWriter = eldest.value
        xmlWriter.closeLogged()
        return true
      }
      return false
    }
  }

  /**
   * Whether it's necessary to save the result to zip.
   */
  private val saveZip = annotationsRoot.extension == "zip"

  /**
   * Original [annotationsRoot] if it is a directory,
   * or a temporary directory.
   */
  private val rootDirectory = if (saveZip) {
    annotationsRoot.resolveSibling(annotationsRoot.simpleName + ".dir")
  } else {
    annotationsRoot
  }

  init {
    //Delete previous root, if exists.
    rootDirectory.deleteLogged()
    rootDirectory.createDir()
  }

  /**
   * Appends the specified [sinceApiData] to the output.
   */
  fun appendSinceApiData(sinceApiData: SinceApiData) {
    val signaturesSequence = sinceApiData.asSequence()
    appendSignatures(signaturesSequence)
  }


  /**
   * Appends the specified sequence of [ApiSignature]s
   * and "available since" versions to the output.
   */
  fun appendSignatures(sequence: Sequence<Pair<ApiSignature, IdeVersion>>) {
    for ((apiSignature, sinceVersion) in sequence) {
      appendSignature(apiSignature, sinceVersion)
    }
  }

  /**
   * Appends the specified signature [apiSignature] available
   * since [sinceVersion] to the output.
   */
  fun appendSignature(apiSignature: ApiSignature, sinceVersion: IdeVersion) {
    getPackageXmlWriter(apiSignature.packageName)
        .appendSignatureSince(apiSignature, sinceVersion)
  }

  private fun getPackageXmlWriter(packageName: String): SinceApiXmlWriter {
    allPackages += packageName
    val existing = packageWriters[packageName]
    if (existing != null) {
      return existing
    }
    val xmlFile = resolveAnnotationsXmlFile(packageName)
    val needStart = !xmlFile.exists() || Files.size(xmlFile) == 0L
    val xmlWriter = SinceApiXmlWriter(xmlFile.createWriter())
    if (needStart) {
      xmlWriter.appendXmlStart()
    }
    packageWriters[packageName] = xmlWriter
    return xmlWriter
  }

  private fun Path.createWriter() = Files.newBufferedWriter(
      this,
      StandardOpenOption.CREATE,
      StandardOpenOption.WRITE,
      StandardOpenOption.APPEND
  )

  private fun resolveAnnotationsXmlFile(packageName: String): Path {
    val packageRoot = rootDirectory.resolve(packageName.replace('.', '/'))
    val annotationsFile = packageRoot.resolve(ANNOTATIONS_XML_FILE_NAME)
    if (!annotationsFile.exists()) {
      annotationsFile.parent.createDir()
    }
    return annotationsFile
  }

  /**
   * Appends endings to XML files of all packages
   * encountered work with `this` writer.
   *
   * Closes all allocated IO resources.
   */
  override fun close() {
    try {
      for (packageName in allPackages) {
        val xmlWriter = getPackageXmlWriter(packageName)
        xmlWriter.appendXmlEnd()
      }
    } finally {
      for (xmlWriter in packageWriters.values) {
        xmlWriter.closeLogged()
      }
      packageWriters.clear()
    }

    if (saveZip) {
      try {
        archiveDirectory(rootDirectory.toFile(), annotationsRoot.toFile(), false)
      } finally {
        rootDirectory.deleteLogged()
      }
    }
  }

}