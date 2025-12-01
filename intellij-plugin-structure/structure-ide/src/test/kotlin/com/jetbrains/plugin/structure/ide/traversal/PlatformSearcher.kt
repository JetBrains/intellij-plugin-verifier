/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.traversal

import com.jetbrains.plugin.structure.base.utils.isJar
import java.io.IOException
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.streams.asSequence

class PlatformSearcher(private val zipEntryFilter: (ZipEntry) -> Boolean = { true }) {
  private fun searchInArchive(archivePath: ZipFilePath): List<Pair<ZipFilePath, URI>> {
    val result = mutableListOf<Pair<ZipFilePath, URI>>()
    ZipFile(archivePath.toFile()).use { zipFile ->
      zipFile.entries().toList().forEach {
        if (zipEntryFilter(it)) {
          result.add(archivePath to archivePath.getUri(it))
        }
      }
    }
    return result
  }

  fun search(ideRoot: Path): Map<ZipFilePath, List<URI>> {
    return Files.walk(ideRoot)
      .filter(Path::isJar)
      .flatMap {
        searchInArchive(it).stream()
      }.asSequence()
      .groupBy { it.first }
      .mapValues { mapEntry ->
        mapEntry.value.map { it.second }
      }
  }

  @Throws(IOException::class)
  private fun ZipFilePath.getUri(zipEntry: ZipEntry): URI {
    val implicitInstalledClassLoader: ClassLoader? = null
    return FileSystems.newFileSystem(/* path = */ this, implicitInstalledClassLoader).use { fs ->
      fs.getPath(zipEntry.name).toUri()
    }
  }
}