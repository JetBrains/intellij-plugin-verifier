/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.downloader

import com.jetbrains.plugin.structure.base.utils.*
import com.jetbrains.pluginverifier.getConcurrencyLevel
import com.jetbrains.pluginverifier.repository.cleanup.fileSize
import com.jetbrains.pluginverifier.repository.provider.ProvideResult
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import org.apache.commons.io.FileUtils
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * [ResourceProvider] responsible for downloading files and directories
 * using provided [downloader] and saving them to the [destinationDirectory]
 * using the [fileNameWithoutExtensionMapper].
 */
class DownloadProvider<in K : Any>(
  private val destinationDirectory: Path,
  private val downloader: Downloader<K>,
  private val fileNameWithoutExtensionMapper: (K) -> String
) : ResourceProvider<K, Path> {
  private companion object {
    const val DOWNLOADS_DIRECTORY = ".downloads"
  }

  private val downloadDirectory = destinationDirectory.resolve(DOWNLOADS_DIRECTORY)

  val downloadStatistics = DownloadStatistics()

  init {
    destinationDirectory.createDir()
    downloadDirectory.forceDeleteIfExists()
  }

  @Throws(InterruptedException::class)
  override fun provide(key: K): ProvideResult<Path> {
    val downloadEvent = downloadStatistics.downloadStarted()
    val tempDirectory = createTempDirectoryForDownload(key)
    try {
      return with(downloader.download(key, tempDirectory)) {
        when (this) {
          is DownloadResult.Downloaded -> {
            downloadEvent.downloadEnded(downloadedFileOrDirectory.fileSize)
            saveDownloadedFileToFinalDestination(key, downloadedFileOrDirectory, extension, isDirectory)
          }
          is DownloadResult.NotFound -> ProvideResult.NotFound(reason)
          is DownloadResult.FailedToDownload -> ProvideResult.Failed(reason, error)
        }
      }
    } finally {
      tempDirectory.deleteLogged()
    }
  }

  private val nameGenerationLocks = Striped.lock(getConcurrencyLevel().coerceAtLeast(1))

  private fun saveDownloadedFileToFinalDestination(
    key: K,
    tempDownloadedFile: Path,
    extension: String,
    isDirectory: Boolean
  ): ProvideResult<Path> {
    // Synchronized to ensure that two threads won't use the same destination file
    val prefixAndSuffix = getDestinationPrefixAndSuffix(key, isDirectory, extension)
    synchronized(nameGenerationLocks.get(prefixAndSuffix)) {
      val destination = getDestinationFile(prefixAndSuffix)
      try {
        moveFileOrDirectory(tempDownloadedFile, destination)
      } catch (e: Exception) {
        e.rethrowIfInterrupted()
        return ProvideResult.Failed("Unable to download $key", e)
      }
      return ProvideResult.Provided(destination)
    }
  }

  private data class PrefixAndSuffix(val nameWithoutExtension: String, val extensionSuffix: String)

  private fun getDestinationPrefixAndSuffix(key: K, isDirectory: Boolean, extension: String): PrefixAndSuffix {
    check(extension == extension.replaceInvalidFileNameCharacters()) { "Extension must not contain invalid characters: $extension" }

    val nameWithoutExtension = fileNameWithoutExtensionMapper(key).replaceInvalidFileNameCharacters()
    val extensionSuffix = if (isDirectory || extension.isEmpty()) "" else ".$extension"

    return PrefixAndSuffix(nameWithoutExtension, extensionSuffix)
  }

  private fun getDestinationFile(name: PrefixAndSuffix): Path {
    var destination = destinationDirectory.resolve(name.nameWithoutExtension + name.extensionSuffix)
    var nextSuffix = 1
    while (Files.exists(destination)) {
      val newName = "${name.nameWithoutExtension} ($nextSuffix)${name.extensionSuffix}"
      destination = destinationDirectory.resolve(newName)
      nextSuffix++
    }
    return destination
  }

  private fun createTempDirectoryForDownload(key: K) =
    try {
      Files.createTempDirectory(
        downloadDirectory.createDir(),
        "download-" + fileNameWithoutExtensionMapper(key).replaceInvalidFileNameCharacters() + "-"
      )
    } catch (e: IOException) {
      throw RuntimeException(e)
    }

  private fun moveFileOrDirectory(fileOrDirectory: Path, destination: Path) {
    if (destination.exists()) {
      destination.deleteLogged()
    }
    if (fileOrDirectory.isDirectory) {
      FileUtils.moveDirectory(fileOrDirectory.toFile(), destination.toFile())
    } else {
      FileUtils.moveFile(fileOrDirectory.toFile(), destination.toFile())
    }
  }

}