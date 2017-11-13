package com.jetbrains.pluginverifier.repository

import com.google.common.primitives.Ints
import com.jetbrains.pluginverifier.misc.closeOnException
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.repository.cleanup.FileSweeper
import com.jetbrains.pluginverifier.repository.downloader.DownloadResult
import com.jetbrains.pluginverifier.repository.downloader.PluginDownloader
import com.jetbrains.pluginverifier.repository.validation.FileValidator
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException

class DownloadManager(val downloadDir: File,
                      private val pluginDownloader: PluginDownloader,
                      private val fileSweeper: FileSweeper,
                      private val fileValidator: FileValidator) {

  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(DownloadManager::class.java)
  }

  private inner class FileLockImpl(val locked: File,
                                   val id: Long,
                                   override val lockTime: Long) : FileLock() {
    override fun getFile(): File = locked

    override fun release() {
      releaseLock(this)
    }

    override fun equals(other: Any?): Boolean = other is FileLockImpl && id == other.id

    override fun hashCode(): Int = id.hashCode()
  }

  private var nextId: Long = 0

  private val locksAcquired: MutableMap<File, MutableSet<FileLockImpl>> = hashMapOf()

  private val busyLocks: MutableMap<Long, FileLockImpl> = hashMapOf()

  //it is not used yet
  private val deleteQueue: MutableSet<File> = hashSetOf()

  @Synchronized
  fun getAvailableFiles(): List<AvailableFile> {
    return downloadDir
        .listFiles()!!
        .filterNot { it.name.startsWith(PluginDownloader.TEMP_PLUGIN_DOWNLOAD_PREFIX) }
        .filter { Ints.tryParse(it.nameWithoutExtension) != null }
        .map {
          AvailableFile(
              it,
              it.length(),
              locksAcquired[it] ?: emptySet()
          )
        }
        .sortedBy { it.size }
  }

  @Synchronized
  fun remove(file: File) {
    if (locksAcquired[file].orEmpty().isEmpty()) {
      file.deleteLogged()
    } else {
      deleteQueue.add(file)
    }
  }

  @Synchronized
  private fun registerLock(pluginFile: File): FileLock {
    val id = nextId++
    val lock = FileLockImpl(pluginFile, id, System.currentTimeMillis())
    locksAcquired.getOrPut(pluginFile, { hashSetOf() }).add(lock)
    busyLocks.put(id, lock)
    return lock
  }

  @Synchronized
  private fun releaseLock(lock: FileLockImpl) {
    if (busyLocks.containsKey(lock.id)) {
      busyLocks.remove(lock.id)
      val fileLocks = locksAcquired[lock.locked]!!
      fileLocks.remove(lock)
      if (fileLocks.isEmpty()) {
        onResourceRelease(lock.locked)
      }
    }
  }

  @Synchronized
  private fun onResourceRelease(updateFile: File) {
    if (updateFile in deleteQueue) {
      deleteQueue.remove(updateFile)
      LOG.debug("Deleting update: $updateFile")
      updateFile.deleteLogged()
    }
  }

  @Synchronized
  private fun getCachedPlugin(updateInfo: UpdateInfo): FileLock? {
    val updateId = updateInfo.updateId
    val asZip = File(downloadDir, "$updateId.zip")
    if (asZip.exists() && fileValidator.isValid(asZip)) {
      return registerLock(asZip)
    }
    val asJar = File(downloadDir, "$updateId.jar")
    if (asJar.exists() && fileValidator.isValid(asJar)) {
      return registerLock(asJar)
    }
    return null
  }

  /**
   *  Provides the thread safety when multiple threads attempt to load the same plugin.
   *
   *  @return true if [tempFile] has been moved to [finalFile], false otherwise
   */
  @Synchronized
  private fun moveDownloaded(tempFile: File, finalFile: File): Boolean {
    if (finalFile.exists()) {
      if (!fileValidator.isValid(finalFile)) {
        FileUtils.forceDelete(finalFile)
      } else {
        return false
      }
    }

    FileUtils.moveFile(tempFile, finalFile)
    return true
  }

  private fun getFinalPluginFile(updateInfo: UpdateInfo, tempFile: File): File {
    val extension = tempFile.extension
    return File(downloadDir, "${updateInfo.updateId}.$extension")
  }

  private fun saveDownloadedPluginToFinalFile(updateInfo: UpdateInfo, tempFile: File): FileLock {
    if (!fileValidator.isValid(tempFile)) {
      throw IOException(getCorruptedMessage(updateInfo, tempFile))
    }
    val pluginFile = getFinalPluginFile(updateInfo, tempFile)
    /*
    We must register the file lock before moving the temp file to the final file because
    there could be another thread which removes the final file before we managed to register a lock for it.
    This is not the case for the temp file: we can operate with the temp file directly because only the current thread has access to it.
     */
    val lock = registerLock(pluginFile)
    val moved = lock.closeOnException {
      moveDownloaded(tempFile, pluginFile)
    }
    if (moved) {
      LOG.debug("Plugin $updateInfo is saved into $pluginFile")
    } else {
      LOG.debug("Another thread has downloaded the same plugin $updateInfo into $pluginFile")
    }
    return lock
  }

  private fun getCorruptedMessage(updateInfo: UpdateInfo, tempFile: File) =
      "Corrupter plugin file $updateInfo: the file size is only ${tempFile.length()} bytes"

  /**
   * Searches the plugin by [updateInfo] coordinates in the local cache and
   * in case it isn't found there, downloads the plugin from the repository.
   *
   * The possible results are represented as subclasses of [DownloadPluginResult].
   * If the plugin is found locally or successfully downloaded, the [FileLock] is registered
   * for the plugin's file so it will be protected against deletions by other threads.
   *
   * This method is thread safe. In case several threads attempt to get the same plugin, only one
   * of them will download it while others will wait for the first to complete. TODO: implement this.
   */
  fun getOrDownloadPlugin(updateInfo: UpdateInfo): DownloadPluginResult {
    val cachedPlugin = getCachedPlugin(updateInfo)
    if (cachedPlugin != null) {
      if (!fileValidator.isValid(cachedPlugin.getFile())) {
        cachedPlugin.release()
      } else {
        return DownloadPluginResult.Found(cachedPlugin)
      }
    }

    val downloadResult = pluginDownloader.download(updateInfo)
    val fileLock = when (downloadResult) {
      is DownloadResult.Downloaded -> {
        try {
          saveDownloadedPluginToFinalFile(updateInfo, downloadResult.file)
        } catch (e: Exception) {
          LOG.debug("Failed to save the downloaded plugin $updateInfo", e)
          return DownloadPluginResult.FailedToDownload("Failed to download plugin $updateInfo" + if (e.message.isNullOrBlank()) "" else ": " + e.message)
        }
      }
      is DownloadResult.FailedToDownload -> {
        return DownloadPluginResult.FailedToDownload(downloadResult.reason)
      }
      is DownloadResult.NotFound -> {
        return DownloadPluginResult.NotFound(downloadResult.reason)
      }
    }

    try {
      fileSweeper.sweep(this)
    } catch (e: Throwable) {
      fileLock.release()
      throw e
    }

    return DownloadPluginResult.Found(fileLock)
  }
}