package org.jetbrains.ide.diff.builder.persistence

import com.jetbrains.plugin.structure.base.utils.archiveDirectory
import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.*
import org.jetbrains.ide.diff.builder.api.ApiEvent
import org.jetbrains.ide.diff.builder.api.ApiReport
import org.jetbrains.ide.diff.builder.signatures.ApiSignature
import java.io.Closeable
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*

/**
 * Utility class used to save [ApiReport] to external annotations roots.
 * [ideBuildNumber] is the IDE build number this root has been built for.
 *
 * Creates necessary package-like directory structure, for example
 * ```
 * build.txt
 * org/
 * org/some/
 * org/some/annotations.xml
 * org/some/util/
 * org/some/util/annotations.xml
 * ```
 *
 * This class is not thread safe.
 */
class ApiReportWriter(private val reportPath: Path, private val ideBuildNumber: IdeVersion) : Closeable {

  private companion object {
    /**
     * Maximum number of files opened for writing at the same time.
     * Limiting is necessary to avoid IO exception: "Too many open files"
     */
    const val MAX_OPEN_FILES = 50
  }

  init {
    require(reportPath.extension == "" || reportPath.extension == "zip") {
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
  private val packageWriters = object : LinkedHashMap<String, ApiXmlWriter>() {
    override fun removeEldestEntry(eldest: Map.Entry<String, ApiXmlWriter>): Boolean {
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
  private val saveZip = reportPath.extension == "zip"

  /**
   * Original [reportPath] if it is a directory,
   * or a temporary directory.
   */
  private val rootDirectory = if (saveZip) {
    reportPath.resolveSibling(reportPath.simpleName + ".dir")
  } else {
    reportPath
  }

  init {
    //Delete previous root, if exists.
    rootDirectory.deleteLogged()
    rootDirectory.createDir()

    rootDirectory.resolve(BUILD_TXT_FILE_NAME).writeText(ideBuildNumber.asStringWithoutProductCode())
  }

  /**
   * Appends the specified [apiReport] to the output.
   */
  fun appendApiReport(apiReport: ApiReport) {
    appendSignatures(apiReport.asSequence())
  }


  /**
   * Appends the specified sequence of signatures and corresponding events to the output.
   */
  private fun appendSignatures(sequence: Sequence<Pair<ApiSignature, ApiEvent>>) {
    for ((apiSignature, apiEvent) in sequence) {
      appendSignature(apiSignature, apiEvent)
    }
  }

  /**
   * Appends the specified signature's event to the output.
   */
  fun appendSignature(apiSignature: ApiSignature, apiEvent: ApiEvent) {
    getPackageXmlWriter(apiSignature.packageName).appendSignature(apiSignature, apiEvent)
  }

  private fun getPackageXmlWriter(packageName: String): ApiXmlWriter {
    allPackages += packageName
    val existing = packageWriters[packageName]
    if (existing != null) {
      return existing
    }
    val xmlFile = resolveAnnotationsXmlFile(packageName)
    val needStart = !xmlFile.exists() || Files.size(xmlFile) == 0L
    val xmlWriter = ApiXmlWriter(xmlFile.createWriter())
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
        archiveDirectory(rootDirectory.toFile(), reportPath.toFile(), false)
      } finally {
        rootDirectory.deleteLogged()
      }
    }
  }

}

fun ApiReport.saveTo(resultPath: Path) {
  ApiReportWriter(resultPath, ideBuildNumber).use { it.appendApiReport(this) }
}