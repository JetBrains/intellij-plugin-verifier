/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.traversal

import com.jetbrains.plugin.structure.base.utils.createParentDirs
import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.formatDuration
import com.jetbrains.plugin.structure.ide.dependencies.PluginXmlDependencyFilter
import com.jetbrains.plugin.structure.ide.dependencies.PluginXmlDependencyFilter.Companion.toByteArray
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.temporal.ChronoUnit.NANOS
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.system.measureNanoTime

typealias ZipFilePath = Path
typealias ZipEntryPath = String

private val LOG: Logger = LoggerFactory.getLogger(TestIdeDumper::class.java)

class TestIdeDumper {
  private val xmlFilter = { it: ZipEntry -> it.name.endsWith(".xml") }
  private val platformXmlSearcher = PlatformSearcher(xmlFilter)
  private val pluginXmlDependencyFilter = PluginXmlDependencyFilter()
  private val additionalFiles = listOf("product-info.json", "build.txt")

  fun dumpIde(ideRoot: Path, targetIdeRoot: Path) {
    platformXmlSearcher
      .search(ideRoot)
      .forEach { (zipFilePath, zipFileEntries) ->
        newZipOutputStream(getTargetPath(ideRoot, targetIdeRoot, zipFilePath)).use { zipStream ->
          zipFileEntries.forEach { zipEntryUri ->
            zipStream.dumpZipEntry(zipFilePath, zipEntryUri)
          }
        }
      }
    dumpAdditionalFiles(ideRoot, targetIdeRoot)
  }

  private fun dumpAdditionalFiles(ideRoot: Path, targetIdeRoot: Path) {
    additionalFiles
      .map { it to ideRoot.resolve(it) }
      .filter { (_, sourcePath) -> sourcePath.exists() }
      .forEach { (fileName, sourcePath) ->
        val targetPath = targetIdeRoot.resolve(fileName)
        try {
          Files.copy(sourcePath, targetPath)
        } catch (e: IOException) {
          LOG.atError().log("Cannot copy {} to {}", sourcePath, targetPath, e)
        }
    }
  }

  private fun getTargetPath(ideRoot: Path, targetIdeRoot: Path, zipFilePath: ZipFilePath): Path {
    val relativeZipFilePath = ideRoot.relativize(zipFilePath)
    return targetIdeRoot.resolve(relativeZipFilePath).also {
      it.createParentDirs()
    }
  }

  private fun newZipOutputStream(zipFilePath: ZipFilePath): ZipOutputStream {
    return ZipOutputStream(FileOutputStream(zipFilePath.toFile()))
  }

  private fun ZipOutputStream.dumpZipEntry(zipFilePath: ZipFilePath, zipEntryUri: URI) {
    zipEntryUri.pathInsideZip?.let { pathInsideZip ->
      newEntry(zipFilePath, pathInsideZip) {
        zipEntryUri.inputStream.use {
          pluginXmlDependencyFilter.toByteArray(it)
        }
      }
    }
  }

  private fun ZipOutputStream.newEntry(zipFilePath: ZipFilePath, pathInsideZip: ZipEntryPath, contentProvider: () -> ByteArray) {
    putNextEntry(ZipEntry(pathInsideZip))
    try {
      write(contentProvider())
    } catch (e: IOException) {
      if (e.suppressedExceptions.isNotEmpty()) {
        val ioe = IOException("Unable to process [$zipFilePath] with path [$pathInsideZip]")
        e.suppressedExceptions.forEach(ioe::addSuppressed)
        throw ioe
      } else {
        throw e
      }
    }
    closeEntry()
  }


  private val URI.pathInsideZip: ZipEntryPath?
    get() {
      with(this.toString()) {
        val separatorPos = indexOf("!")
        return if (0 < separatorPos && separatorPos < length - 1) {
          substring(separatorPos + 1).removePrefix("/")
        } else {
          null
        }
      }
    }

  private val URI.inputStream: InputStream
    @Throws(IOException::class)
    get() = toURL().openStream()
}

fun main(args: Array<String>) {
  if (args.size != 2) {
    println("Usage: <IDE path> <target IDE path>")
    return
  }
  val idePath = Path.of(args[0])
  val targetIdePath = Path.of(args[1])
  val ideDumper = TestIdeDumper()
  measureNanoTime {
    println("Dumping $idePath to $targetIdePath")
    ideDumper.dumpIde(idePath, targetIdePath)
  }.let {
    val duration = Duration.of(it, NANOS).formatDuration()
    println("Done dumping $idePath to $targetIdePath in $duration")
  }
}