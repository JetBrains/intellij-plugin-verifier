package com.jetbrains.pluginverifier.repository

import com.google.common.primitives.Ints
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.jetbrains.pluginverifier.misc.bytesToMegabytes
import com.jetbrains.pluginverifier.misc.closeOnException
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.repository.downloader.DownloadResult
import com.jetbrains.pluginverifier.repository.downloader.PluginDownloader
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DownloadManager(private val downloadDir: File,
                      downloadDirMaxSpace: Long,
                      private val pluginDownloader: PluginDownloader) {

  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(DownloadManager::class.java)

    val FORGOTTEN_LOCKS_GC_TIMEOUT_MS: Long = TimeUnit.HOURS.toMillis(8)

    val BROKEN_FILE_THRESHOLD_BYTES = 200

    fun File.isCorrupterPluginFile(): Boolean = length() < BROKEN_FILE_THRESHOLD_BYTES
  }

  private inner class FileLockImpl(val locked: File, val id: Long, val lockDate: Long) : FileLock() {
    override fun getFile(): File = locked

    override fun release() {
      releaseLock(this)
    }
  }

  init {
    Executors.newSingleThreadScheduledExecutor(
        ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("download-mng-gc-%d")
            .build()
    ).scheduleAtFixedRate({ removeUnusedPlugins() }, 5, 30, TimeUnit.SECONDS)
  }

  private var nextId: Long = 0

  private val locksAcquired: MutableMap<File, Int> = hashMapOf()

  private val busyLocks: MutableMap<Long, FileLockImpl> = hashMapOf()

  //it is not used yet
  private val deleteQueue: MutableSet<File> = hashSetOf()

  private val spaceWatcher = FreeDiskSpaceWatcher(downloadDir, downloadDirMaxSpace)

  @Synchronized
  private fun removeUnusedPlugins() {
    val spaceReport = spaceWatcher.getSpaceReport()
    LOG.info("It's time to remove unused plugins from cache. Download cache usage: ${spaceReport.usedSpace.bytesToMegabytes()} Mb; " +
        "Estimated available space (Mb): ${spaceReport.availableSpace.bytesToMegabytes()}")

    releaseOldLocks()
    val newSpaceReport = spaceWatcher.getSpaceReport()
    if (newSpaceReport.availableSpace < newSpaceReport.lowSpaceThreshold) {
      LOG.warn("Available space is less than a recommended threshold: $newSpaceReport")
      deleteUnusedPlugins()
    }
  }

  private fun deleteUnusedPlugins() {
    val updatesToDelete = downloadDir
        .listFiles()!!
        .filterNot { it.name.startsWith(PluginDownloader.TEMP_PLUGIN_DOWNLOAD_PREFIX) }
        .filterNot { it in locksAcquired }
        .filter { Ints.tryParse(it.nameWithoutExtension) != null }
        .sortedByDescending { it.length() }

    for (update in updatesToDelete) {
      val spaceReport = spaceWatcher.getSpaceReport()
      if (spaceReport.availableSpace > spaceReport.enoughSpaceThreshold) {
        LOG.info("Enough space after cleanup ${spaceReport.availableSpace.bytesToMegabytes()} Mb > ${spaceReport.enoughSpaceThreshold.bytesToMegabytes()} Mb")
        break
      }
      LOG.info("Deleting unused update $update of size ${update.length().bytesToMegabytes()} Mb")
      update.deleteLogged()
    }

    val spaceReport = spaceWatcher.getSpaceReport()
    if (spaceReport.availableSpace < spaceReport.lowSpaceThreshold) {
      LOG.warn("Available space after cleanup is not sufficient! ${spaceReport.availableSpace.bytesToMegabytes()} Mb < ${spaceReport.lowSpaceThreshold.bytesToMegabytes()} Mb")
    }
  }

  private fun releaseOldLocks() {
    busyLocks.values
        .filter { System.currentTimeMillis() - it.lockDate > FORGOTTEN_LOCKS_GC_TIMEOUT_MS }
        .forEach {
          LOG.warn("Forgotten lock found: $it; lock date = ${Date(it.lockDate)}")
          releaseLock(it)
        }
  }

  @Synchronized
  private fun registerLock(pluginFile: File): FileLock {
    val id = nextId++
    val cnt = locksAcquired.getOrPut(pluginFile, { 0 })
    locksAcquired.put(pluginFile, cnt + 1)
    val lock = FileLockImpl(pluginFile, id, System.currentTimeMillis())
    busyLocks.put(id, lock)
    return lock
  }

  @Synchronized
  private fun releaseLock(lock: FileLockImpl) {
    if (busyLocks.containsKey(lock.id)) {
      busyLocks.remove(lock.id)
      val cnt = locksAcquired[lock.locked]!!
      if (cnt == 1) {
        locksAcquired.remove(lock.locked)
        onResourceRelease(lock.locked)
      } else {
        locksAcquired[lock.locked] = cnt - 1
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
    if (asZip.exists() && !asZip.isCorrupterPluginFile()) {
      return registerLock(asZip)
    }
    val asJar = File(downloadDir, "$updateId.jar")
    if (asJar.exists() && !asJar.isCorrupterPluginFile()) {
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
      if (finalFile.isCorrupterPluginFile()) {
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
    if (tempFile.isCorrupterPluginFile()) {
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
      if (cachedPlugin.getFile().isCorrupterPluginFile()) {
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
      val spaceReport = spaceWatcher.getSpaceReport()
      if (spaceReport.availableSpace < spaceReport.lowSpaceThreshold) {
        removeUnusedPlugins()
      }
    } catch (e: Throwable) {
      fileLock.release()
      throw e
    }

    return DownloadPluginResult.Found(fileLock)
  }
}