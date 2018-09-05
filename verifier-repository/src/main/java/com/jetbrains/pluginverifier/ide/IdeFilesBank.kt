package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.IdeFilesBank.Result.Found
import com.jetbrains.pluginverifier.misc.isDirectory
import com.jetbrains.pluginverifier.misc.simpleName
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import com.jetbrains.pluginverifier.repository.cleanup.LruFileSizeSweepPolicy
import com.jetbrains.pluginverifier.repository.downloader.DownloadProvider
import com.jetbrains.pluginverifier.repository.files.*
import com.jetbrains.pluginverifier.repository.provider.ProvideResult
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import java.nio.file.Path

/**
 * Storage of IDE builds, which are kept under the [bankDirectory].
 *
 * Each IDE is identified by its [IdeVersion] and can be locked for the use time
 * to avoid use-remove conflicts when one thread uses the IDE build and another
 * thread deletes it.
 */
class IdeFilesBank(
    private val bankDirectory: Path,
    ideRepository: IdeRepository,
    diskSpaceSetting: DiskSpaceSetting
) {

  private val ideFilesRepository = FileRepositoryBuilder<IdeVersion>()
      .sweepPolicy(LruFileSizeSweepPolicy(diskSpaceSetting))
      .resourceProvider(IdeDownloadProvider(bankDirectory, ideRepository))
      .presentableName("IDEs bank at $bankDirectory")
      .addInitialFilesFrom(bankDirectory) { getIdeVersionByPath(it) }
      .build()

  private fun getIdeVersionByPath(file: Path) =
      if (file.isDirectory) {
        IdeVersion.createIdeVersionIfValid(file.simpleName)
            ?.setProductCodeIfAbsent("IU")
      } else {
        null
      }

  fun getAvailableIdeVersions(): Set<IdeVersion> =
      ideFilesRepository.getAllExistingKeys()

  fun getAvailableIdeFiles(): List<AvailableFile<IdeVersion>> =
      ideFilesRepository.getAvailableFiles()

  fun isAvailable(ideVersion: IdeVersion): Boolean =
      ideFilesRepository.has(ideVersion)

  fun deleteIde(ideVersion: IdeVersion): Boolean =
      ideFilesRepository.remove(ideVersion)

  @Throws(InterruptedException::class)
  fun getIdeFile(ideVersion: IdeVersion): Result =
      with(ideFilesRepository.getFile(ideVersion)) {
        when (this) {
          is FileRepositoryResult.Found -> Result.Found(lockedFile)
          is FileRepositoryResult.NotFound -> Result.NotFound(reason)
          is FileRepositoryResult.Failed -> Result.Failed(reason, error)
        }
      }

  /**
   * Result of [getting] [getIdeFile] IDE file.
   *
   * [Found] result contains a [FileLock] that must
   * be closed after usage.
   */
  sealed class Result {
    /**
     * IDE build is found.
     *
     * [ideFileLock] is registered for this IDE
     * to protect it from deletions while it is used.
     * It must be [closed] [FileLock.close] after usage.
     */
    data class Found(val ideFileLock: FileLock) : Result()

    /**
     * IDE is not found due to [reason].
     */
    data class NotFound(val reason: String) : Result()

    /**
     * IDE is failed to be found because of [reason]
     * and an aroused [exception].
     */
    data class Failed(val reason: String, val exception: Exception) : Result()
  }

  override fun toString() = "IDEs at $bankDirectory"

}

private class IdeDownloadProvider(
    bankDirectory: Path,
    val ideRepository: IdeRepository
) : ResourceProvider<IdeVersion, Path> {

  private val ideFileNameMapper = object : FileNameMapper<AvailableIde> {
    override fun getFileNameWithoutExtension(key: AvailableIde) =
        key.version.asString()
  }

  private val downloadProvider = DownloadProvider(bankDirectory, IdeDownloader(), ideFileNameMapper)

  override fun provide(key: IdeVersion): ProvideResult<Path> {
    val availableIde = try {
      ideRepository.fetchAvailableIde(key)
    } catch (ie: InterruptedException) {
      throw ie
    } catch (e: Exception) {
      return ProvideResult.Failed("Failed to find IDE $key ", e)
    } ?: return ProvideResult.NotFound("IDE $key is not available")

    return downloadProvider.provide(availableIde)
  }
}
