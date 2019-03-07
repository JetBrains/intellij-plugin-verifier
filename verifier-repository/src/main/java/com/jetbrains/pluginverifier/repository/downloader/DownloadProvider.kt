package com.jetbrains.pluginverifier.repository.downloader

import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.pluginverifier.misc.*
import com.jetbrains.pluginverifier.repository.cleanup.fileSize
import com.jetbrains.pluginverifier.repository.files.FileNameMapper
import com.jetbrains.pluginverifier.repository.provider.ProvideResult
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import org.apache.commons.io.FileUtils
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * [ResourceProvider] responsible for downloading files and directories
 * using provided [downloader] and saving them to the [destinationDirectory]
 * using the [fileNameMapper].
 */
class DownloadProvider<in K>(
    private val destinationDirectory: Path,
    private val downloader: Downloader<K>,
    private val fileNameMapper: FileNameMapper<K>
) : ResourceProvider<K, Path> {
  private companion object {
    const val DOWNLOADS_DIRECTORY = "downloads"
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

  @Synchronized
  private fun saveDownloadedFileToFinalDestination(
      key: K,
      tempDownloadedFile: Path,
      extension: String,
      isDirectory: Boolean
  ): ProvideResult<Path> {
    val destination = createDestinationFileForKey(key, extension, isDirectory)
    try {
      moveFileOrDirectory(tempDownloadedFile, destination)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      return ProvideResult.Failed("Unable to download $key", e)
    }
    return ProvideResult.Provided(destination)
  }

  private fun createTempDirectoryForDownload(key: K) =
      try {
        Files.createTempDirectory(
            downloadDirectory.createDir(),
            "download-" + getFileNameForKey(key, "", true) + "-"
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

  private fun createDestinationFileForKey(key: K, extension: String, isDirectory: Boolean): Path {
    val finalFileName = getFileNameForKey(key, extension, isDirectory)
    return destinationDirectory.resolve(finalFileName)
  }

  private fun getFileNameForKey(key: K, extension: String, isDirectory: Boolean): String {
    val nameWithoutExtension = fileNameMapper.getFileNameWithoutExtension(key)
    if (nameWithoutExtension == DOWNLOADS_DIRECTORY) {
      throw IllegalStateException("File or directory named '$DOWNLOADS_DIRECTORY' is prohibited")
    }
    val fullName = nameWithoutExtension + if (isDirectory || extension.isEmpty()) "" else "." + extension
    return fullName.replaceInvalidFileNameCharacters()
  }

}