package org.jetbrains.ide.diff.builder.persistence

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.*
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.NameFileFilter
import org.apache.commons.io.filefilter.TrueFileFilter
import org.jetbrains.ide.diff.builder.api.ApiData
import org.jetbrains.ide.diff.builder.api.ApiEvent
import org.jetbrains.ide.diff.builder.api.ApiReport
import org.jetbrains.ide.diff.builder.signatures.ApiSignature
import java.io.Closeable
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Utility class used to read [ApiReport] from [reportPath], which may be a .zip or directory.
 *
 * This class is not thread-safe.
 */
class ApiReportReader(private val reportPath: Path) : Closeable {

  companion object {
    fun readFrom(reportPath: Path) = ApiReportReader(reportPath).use { it.readApiReport() }
  }

  /**
   * Sequence of [ApiXmlReader] for the given [reportPath].
   * XML readers of this sequence get closed after fully processed.
   */
  private val xmlReaderSequence: XmlReaderSequence

  /**
   * XML reader used to read signature of the current "annotations.xml".
   */
  private var currentXmlReader: ApiXmlReader? = null

  init {
    require(reportPath.isDirectory || reportPath.extension == "zip") {
      "Only directory or .zip roots are supported"
    }
    xmlReaderSequence = buildXmlReaderSequence()
  }

  /**
   * IDE build number this root was built for.
   */
  fun readIdeBuildNumber(): IdeVersion {
    val buildNumberStr = if (reportPath.extension == "zip") {
      ZipFile(reportPath.toFile()).use {
        val entry = it.getEntry(BUILD_TXT_FILE_NAME)
            ?: throw IllegalArgumentException("$reportPath must contain $BUILD_TXT_FILE_NAME")
        it.getInputStream(entry).bufferedReader().readLine()
      }
    } else {
      reportPath.resolve(BUILD_TXT_FILE_NAME).readText()
    }
    return IdeVersion.createIdeVersionIfValid(buildNumberStr)
        ?: throw IllegalArgumentException("Invalid IDE build number $buildNumberStr written in $BUILD_TXT_FILE_NAME of $reportPath")
  }

  private fun buildXmlReaderSequence(): XmlReaderSequence =
      if (reportPath.extension == "zip") {
        ZipXmlReaderSequence(ZipFile(reportPath.toFile()))
      } else {
        val xmlFiles = FileUtils.listFiles(
            reportPath.toFile(),
            NameFileFilter(ANNOTATIONS_XML_FILE_NAME),
            TrueFileFilter.INSTANCE
        ).map { it.toPath().toAbsolutePath() }
        FilesXmlReaderSequence(reportPath.toAbsolutePath(), xmlFiles)
      }

  /**
   * Reads external annotations from [reportPath] and returns corresponding [ApiReport].
   */
  fun readApiReport(): ApiReport {
    val ideBuildNumber = readIdeBuildNumber()
    val apiEventToData = mutableMapOf<ApiEvent, ApiData>()
    for ((apiSignature, apiEvent) in readAllSignatures()) {
      apiEventToData.getOrPut(apiEvent) { ApiData() }
          .addSignature(apiSignature)
    }
    return ApiReport(ideBuildNumber, apiEventToData)
  }

  /**
   * Sequence of all signatures and corresponding API events recorded in the configured annotations root.
   */
  fun readAllSignatures(): Sequence<Pair<ApiSignature, ApiEvent>> =
      generateSequence { readNextSignature() }

  private fun readNextSignature(): Pair<ApiSignature, ApiEvent>? {
    while (true) {
      if (currentXmlReader == null) {
        currentXmlReader = xmlReaderSequence.getNextReader()
        if (currentXmlReader == null) {
          return null
        }
      }

      val nextSignature = currentXmlReader!!.readNextSignature()
      if (nextSignature != null) {
        return nextSignature
      } else {
        currentXmlReader!!.closeLogged()
        currentXmlReader = null
      }
    }
  }

  override fun close() {
    currentXmlReader?.closeLogged()
    xmlReaderSequence.closeLogged()
  }

}

/**
 * Iterable sequence of [ApiXmlReader]s from a specific root.
 * - from a zip file - [ZipXmlReaderSequence]
 * - from multiple files - [FilesXmlReaderSequence]
 *
 * On [close], all allocated resources will be released. For [ZipXmlReaderSequence]
 * the ZipFile will be closed.
 */
private interface XmlReaderSequence : Closeable {
  fun getNextReader(): ApiXmlReader?
}

private class FilesXmlReaderSequence(
    private val annotationsRoot: Path,
    xmlFiles: List<Path>
) : XmlReaderSequence {

  private val filesIterator = xmlFiles.iterator()

  override fun getNextReader(): ApiXmlReader? {
    if (filesIterator.hasNext()) {
      val nextFile = filesIterator.next()
      val packageName = annotationsRoot
          .toAbsolutePath()
          .relativize(nextFile.toAbsolutePath())
          .toString()
          .toSystemIndependentName()
          .substringBeforeLast('/', "")
          .replace('/', '.')

      return Files.newBufferedReader(nextFile).closeOnException {
        ApiXmlReader(packageName, it)
      }
    }
    return null
  }

  override fun close() = Unit
}

private class ZipXmlReaderSequence(val zipFile: ZipFile) : XmlReaderSequence {

  private val zipEntries = zipFile.entries()

  private fun getNextXmlEntry(): ZipEntry? {
    while (zipEntries.hasMoreElements()) {
      val zipEntry = zipEntries.nextElement()
      if (zipEntry.name.toSystemIndependentName().endsWith("/$ANNOTATIONS_XML_FILE_NAME")) {
        return zipEntry
      }
    }
    return null
  }

  override fun getNextReader(): ApiXmlReader? {
    val xmlEntry = getNextXmlEntry() ?: return null
    val packageName = xmlEntry.name
        .toSystemIndependentName()
        .trimStart('/')
        .substringBeforeLast("/")
        .replace('/', '.')
    return zipFile.getInputStream(xmlEntry).bufferedReader().closeOnException {
      ApiXmlReader(packageName, it)
    }
  }

  override fun close() {
    zipFile.close()
  }

}