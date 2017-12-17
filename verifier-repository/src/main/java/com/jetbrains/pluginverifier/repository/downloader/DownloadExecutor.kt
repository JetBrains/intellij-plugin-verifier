package com.jetbrains.pluginverifier.repository.downloader

import com.jetbrains.pluginverifier.misc.*
import com.jetbrains.pluginverifier.repository.files.FileNameMapper
import org.apache.commons.io.FileUtils
import java.nio.file.Files
import java.nio.file.Path

/**
 * Download executor is responsible for downloading files and directories using
 * provided [downloader] and saving them to the [destinationDirectory] using provided [fileNameMapper].
 */
class DownloadExecutor<in K>(private val destinationDirectory: Path,
                             private val downloader: Downloader<K>,
                             private val fileNameMapper: FileNameMapper<K>) {

  private companion object {
    const val DOWNLOADS_DIRECTORY = "downloads"
  }

  private val downloadDirectory = destinationDirectory.resolve(DOWNLOADS_DIRECTORY)

  init {
    destinationDirectory.createDir()
    downloadDirectory.forceDeleteIfExists()
  }

  fun getAvailableFiles() = destinationDirectory.listFiles()

  fun download(key: K): DownloadResult {
    val tempDirectory = createTempDirectoryForDownload(key)
    try {
      val downloadResult = downloader.download(key, tempDirectory)
      if (downloadResult is DownloadResult.Downloaded) {
        return saveDownloadedFileToFinalDestination(key, downloadResult.downloadedFileOrDirectory, downloadResult.extension, downloadResult.isDirectory)
      }
      return downloadResult
    } finally {
      tempDirectory.deleteLogged()
    }
  }

  @Synchronized
  private fun saveDownloadedFileToFinalDestination(key: K,
                                                   tempDownloadedFile: Path,
                                                   extension: String,
                                                   isDirectory: Boolean): DownloadResult {
    val destination = createDestinationFileForKey(key, extension, isDirectory)
    try {
      moveFileOrDirectory(tempDownloadedFile, destination)
    } catch (e: Exception) {
      return DownloadResult.FailedToDownload("Unable to download $key", e)
    }
    return DownloadResult.Downloaded(destination, extension, isDirectory)
  }

  @Synchronized
  private fun createTempDirectoryForDownload(key: K) = Files.createTempDirectory(
      downloadDirectory.createDir(),
      "download-" + getFileNameForKey(key, "", true) + "-"
  )

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