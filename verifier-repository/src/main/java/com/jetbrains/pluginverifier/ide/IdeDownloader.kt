package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.misc.extractTo
import com.jetbrains.pluginverifier.repository.downloader.DownloadResult
import com.jetbrains.pluginverifier.repository.downloader.Downloader
import com.jetbrains.pluginverifier.repository.downloader.UrlDownloader
import java.nio.file.Files
import java.nio.file.Path

/**
 * [Downloader] of the IDEs from the [IdeRepository].
 */
class IdeDownloader(private val ideRepository: IdeRepository) : Downloader<IdeVersion> {

  private val urlDownloader = UrlDownloader<IdeVersion> { getIdeDownloadUrl(it) }

  private fun getIdeDownloadUrl(key: IdeVersion) =
      try {
        ideRepository.fetchAvailableIdeDescriptor(key)?.downloadUrl
      } catch (e: Exception) {
        null
      }

  override fun download(key: IdeVersion, tempDirectory: Path): DownloadResult {
    val downloadResult = downloadIdeToTempFile(key, tempDirectory)
    if (downloadResult !is DownloadResult.Downloaded) {
      return downloadResult
    }
    return try {
      require(downloadResult.extension == "zip") { "IDE repository must provide .zip-ed IDE but provided: ${downloadResult.downloadedFileOrDirectory}" }
      extractIde(downloadResult.downloadedFileOrDirectory, tempDirectory, key)
    } finally {
      downloadResult.downloadedFileOrDirectory.deleteLogged()
    }
  }

  private fun downloadIdeToTempFile(ideVersion: IdeVersion, tempDirectory: Path): DownloadResult {
    return try {
      with(urlDownloader.download(ideVersion, tempDirectory)) {
        when (this) {
          is DownloadResult.Downloaded -> this
          is DownloadResult.NotFound -> DownloadResult.NotFound("IDE $ideVersion is not found in $ideRepository: $reason")
          is DownloadResult.FailedToDownload -> DownloadResult.FailedToDownload("Failed to download IDE $ideVersion: $reason", error)
        }
      }
    } catch (e: Exception) {
      DownloadResult.FailedToDownload("Unable to download $ideVersion", e)
    }
  }

  private fun extractIde(zippedIde: Path, tempDirectory: Path, ideVersion: IdeVersion): DownloadResult {
    val destinationDir = Files.createTempDirectory(tempDirectory, "")
    return try {
      zippedIde.toFile().extractTo(destinationDir.toFile())
      DownloadResult.Downloaded(destinationDir, "", true)
    } catch (e: Exception) {
      destinationDir.deleteLogged()
      DownloadResult.FailedToDownload("Unable to extract zip file of $ideVersion", e)
    } catch (e: Throwable) {
      destinationDir.deleteLogged()
      throw e
    }
  }

}