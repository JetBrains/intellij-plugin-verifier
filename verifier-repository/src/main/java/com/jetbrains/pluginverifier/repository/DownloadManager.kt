package com.jetbrains.pluginverifier.repository

import com.google.common.primitives.Ints
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.jetbrains.pluginverifier.misc.bytesToMegabytes
import com.jetbrains.pluginverifier.misc.closeOnException
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.network.NotFound404ResponseException
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DownloadManager(private val downloadDir: File,
                      downloadDirMaxSpace: Long,
                      private val downloader: (UpdateInfo) -> Response<ResponseBody>) {

  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(DownloadManager::class.java)

    val TEMP_DOWNLOAD_PREFIX = "plugin_"

    val TEMP_DOWNLOAD_SUFFIX = "_download"

    val FORGOTTEN_LOCKS_GC_TIMEOUT_MS: Long = TimeUnit.HOURS.toMillis(8)

    val BROKEN_FILE_THRESHOLD_BYTES = 200

    fun File.isCorrupterPluginFile(): Boolean = length() < BROKEN_FILE_THRESHOLD_BYTES

    val JAR_CONTENT_TYPE = MediaType.parse("application/java-archive")
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
        .filterNot { it.name.startsWith(TEMP_DOWNLOAD_PREFIX) && it.name.endsWith(TEMP_DOWNLOAD_SUFFIX) }
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

  private fun doDownloadAndGuessPluginFileName(updateInfo: UpdateInfo, tempFile: File): String {
    val updateId = updateInfo.updateId
    val response = downloader(updateInfo)
    FileUtils.copyInputStreamToFile(response.body().byteStream(), tempFile)
    val extension = guessExtension(response)
    return "$updateId.$extension"
  }

  private fun guessExtension(response: Response<ResponseBody>): String =
      if (response.body().contentType() == JAR_CONTENT_TYPE) {
        "jar"
      } else {
        "zip"
      }

  /**
   *  Provides the thread safety when multiple threads attempt to load the same plugin.
   *
   *  @return true if [tempFile] has been moved to [finalFile], false otherwise
   */
  @Synchronized
  private fun moveDownloaded(tempFile: File, finalFile: File, updateInfo: UpdateInfo): Boolean {
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

  private fun downloadPlugin(updateInfo: UpdateInfo, tempFile: File): FileLock {
    val guessedFileName = doDownloadAndGuessPluginFileName(updateInfo, tempFile)
    if (tempFile.isCorrupterPluginFile()) {
      throw IOException(getCorruptedMessage(updateInfo, tempFile))
    }
    val pluginFile = File(downloadDir, guessedFileName)
    /*
    We must register the file lock before moving the temp file to the final file because
    there could be another thread which removes the final file before we managed to register a lock for it.
    This is not the case for the temp file: we can operate with the temp file directly because only the current thread has access to it.
     */
    val lock = registerLock(pluginFile)
    val moved = lock.closeOnException {
      moveDownloaded(tempFile, pluginFile, updateInfo)
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

  private fun downloadPlugin(updateInfo: UpdateInfo): FileLock {
    val tempFile = File.createTempFile(TEMP_DOWNLOAD_PREFIX, TEMP_DOWNLOAD_SUFFIX, downloadDir)
    LOG.debug("Downloading update $updateInfo to $tempFile")
    try {
      return downloadPlugin(updateInfo, tempFile)
    } finally {
      tempFile.deleteLogged()
    }
  }

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

    val downloadedPlugin = try {
      downloadPlugin(updateInfo)
    } catch (e: NotFound404ResponseException) {
      return DownloadPluginResult.NotFound("Plugin $updateInfo is not found the Plugin Repository")
    } catch (e: Exception) {
      //todo: provide a human readable error message: maybe find a library capable of this?
      val message = "Unable to download plugin $updateInfo" + (if (e.message != null) " " + e.message else "")
      LOG.info(message, e)
      return DownloadPluginResult.FailedToDownload(message + ": " + e.message)
    }

    try {
      val spaceReport = spaceWatcher.getSpaceReport()
      if (spaceReport.availableSpace < spaceReport.lowSpaceThreshold) {
        removeUnusedPlugins()
      }
    } catch (e: Throwable) {
      downloadedPlugin.release()
      throw e
    }

    return DownloadPluginResult.Found(downloadedPlugin)
  }
}