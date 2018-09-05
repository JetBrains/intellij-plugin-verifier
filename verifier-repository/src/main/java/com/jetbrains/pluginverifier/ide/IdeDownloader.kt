package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.base.utils.extractTo
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.misc.stripTopLevelDirectory
import com.jetbrains.pluginverifier.repository.downloader.DownloadResult
import com.jetbrains.pluginverifier.repository.downloader.Downloader
import com.jetbrains.pluginverifier.repository.downloader.UrlDownloader
import java.nio.file.Files
import java.nio.file.Path

/**
 * [Downloader] of the IDEs from the [IdeRepository].
 */
class IdeDownloader : Downloader<AvailableIde> {

  private val urlDownloader = UrlDownloader<AvailableIde> { it.downloadUrl }

  @Throws(InterruptedException::class)
  override fun download(key: AvailableIde, tempDirectory: Path): DownloadResult {
    return try {
      downloadIde(key, key.version, tempDirectory)
    } catch (ie: InterruptedException) {
      throw ie
    } catch (e: Exception) {
      DownloadResult.FailedToDownload("Unable to download $key", e)
    }
  }

  private fun downloadIde(
      availableIde: AvailableIde,
      ideVersion: IdeVersion,
      tempDirectory: Path
  ) = with(urlDownloader.download(availableIde, tempDirectory)) {
    when (this) {
      is DownloadResult.Downloaded -> {
        try {
          extractIdeToTempDir(downloadedFileOrDirectory, tempDirectory)
        } finally {
          downloadedFileOrDirectory.deleteLogged()
        }
      }
      is DownloadResult.NotFound -> DownloadResult.NotFound("IDE $ideVersion is not found: $reason")
      is DownloadResult.FailedToDownload -> DownloadResult.FailedToDownload("Failed to download IDE $ideVersion: $reason", error)
    }
  }

  private fun extractIdeToTempDir(archivedIde: Path, tempDirectory: Path): DownloadResult {
    val destinationDir = Files.createTempDirectory(tempDirectory, "")
    return try {
      archivedIde.toFile().extractTo(destinationDir.toFile())
      /**
       * Some IDE builds (like MPS) are distributed in form
       * of `<build>.zip/<single>/...`
       * where the <single> is the only directory under .zip.
       */
      stripTopLevelDirectory(destinationDir)
      DownloadResult.Downloaded(destinationDir, "", true)
    } catch (e: Throwable) {
      destinationDir.deleteLogged()
      throw e
    }
  }

}