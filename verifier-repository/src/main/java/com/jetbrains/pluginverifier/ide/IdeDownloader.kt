package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.checkIfInterrupted
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.misc.extractTo
import com.jetbrains.pluginverifier.misc.stripTopLevelDirectory
import com.jetbrains.pluginverifier.repository.downloader.DownloadResult
import com.jetbrains.pluginverifier.repository.downloader.Downloader
import com.jetbrains.pluginverifier.repository.downloader.UrlDownloader
import java.nio.file.Files
import java.nio.file.Path

/**
 * [Downloader] of the IDEs from the [IdeRepository].
 */
class IdeDownloader(private val ideRepository: IdeRepository) : Downloader<IdeVersion> {

  private val urlDownloader = UrlDownloader<AvailableIde> { it.downloadUrl }

  @Throws(InterruptedException::class)
  override fun download(key: IdeVersion, tempDirectory: Path): DownloadResult {
    val availableIde = try {
      ideRepository.fetchAvailableIde(key)
    } catch (ie: InterruptedException) {
      throw ie
    } catch (e: Exception) {
      return DownloadResult.FailedToDownload("Failed to find IDE $key ", e)
    } ?: return DownloadResult.NotFound("IDE $key is not available")

    return downloadIde(availableIde, key, tempDirectory)
  }

  private fun downloadIde(
      availableIde: AvailableIde,
      ideVersion: IdeVersion,
      tempDirectory: Path
  ) = try {
    with(urlDownloader.download(availableIde, tempDirectory)) {
      when (this) {
        is DownloadResult.Downloaded -> {
          try {
            extractIdeToTempDir(downloadedFileOrDirectory, tempDirectory, ideVersion)
          } finally {
            downloadedFileOrDirectory.deleteLogged()
          }
        }
        is DownloadResult.NotFound -> DownloadResult.NotFound("IDE $ideVersion is not found in $ideRepository: $reason")
        is DownloadResult.FailedToDownload -> DownloadResult.FailedToDownload("Failed to download IDE $ideVersion: $reason", error)
      }
    }
  } catch (ie: InterruptedException) {
    throw ie
  } catch (e: Exception) {
    checkIfInterrupted()
    DownloadResult.FailedToDownload("Unable to download $ideVersion", e)
  }

  private fun extractIdeToTempDir(archivedIde: Path, tempDirectory: Path, ideVersion: IdeVersion): DownloadResult {
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
    } catch (ie: InterruptedException) {
      destinationDir.deleteLogged()
      throw ie
    } catch (e: Exception) {
      destinationDir.deleteLogged()
      checkIfInterrupted()
      DownloadResult.FailedToDownload("Unable to extract zip file of $ideVersion", e)
    } catch (e: Throwable) {
      checkIfInterrupted()
      destinationDir.deleteLogged()
      throw e
    }
  }

}