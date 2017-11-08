package com.jetbrains.pluginverifier.repository

import com.google.common.primitives.Ints
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.jetbrains.pluginverifier.misc.bytesToMegabytes
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

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(DownloadManager::class.java)

    private val TEMP_DOWNLOAD_PREFIX = "plugin_"

    private val TEMP_DOWNLOAD_SUFFIX = "_download"

    private val FORGOTTEN_LOCKS_GC_TIMEOUT_MS: Long = TimeUnit.HOURS.toMillis(8)

    private val BROKEN_FILE_THRESHOLD_BYTES = 200

    private val JAR_CONTENT_TYPE = MediaType.parse("application/java-archive")
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
    ).scheduleAtFixedRate({ releaseOldLocksAndDeleteUnusedPlugins() }, 5, 30, TimeUnit.SECONDS)
  }

  private var nextId: Long = 0

  private val locksAcquired: MutableMap<File, Int> = hashMapOf()

  private val busyLocks: MutableMap<Long, FileLockImpl> = hashMapOf()

  //it is not used yet
  private val deleteQueue: MutableSet<File> = hashSetOf()

  private val spaceWatcher = FreeDiskSpaceWatcher(downloadDir, downloadDirMaxSpace)

  @Synchronized
  private fun releaseOldLocksAndDeleteUnusedPlugins() {
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

  private fun getCachedFile(updateInfo: UpdateInfo): File? {
    val updateId = updateInfo.updateId
    val asZip = File(downloadDir, "$updateId.zip")
    if (asZip.exists() && asZip.length() >= BROKEN_FILE_THRESHOLD_BYTES) {
      return asZip
    }
    val asJar = File(downloadDir, "$updateId.jar")
    if (asJar.exists() && asJar.length() >= BROKEN_FILE_THRESHOLD_BYTES) {
      return asJar
    }
    return null
  }

  private fun doDownloadAndGuessFileName(updateInfo: UpdateInfo, tempFile: File): String {
    val updateId = updateInfo.updateId
    val response = downloader(updateInfo)
    FileUtils.copyInputStreamToFile(response.body().byteStream(), tempFile)
    val extension = guessExtension(response)
    return "$updateId.$extension"
  }

  private fun guessExtension(response: Response<ResponseBody>): String {
    if (response.body().contentType() == JAR_CONTENT_TYPE) {
      return "jar"
    }
    return "zip"
  }

  private fun downloadToTempFile(updateInfo: UpdateInfo, tempFile: File): String {
    val updateFileName = doDownloadAndGuessFileName(updateInfo, tempFile)
    if (tempFile.length() < BROKEN_FILE_THRESHOLD_BYTES) {
      throw IOException("Too small update $updateInfo size: ${tempFile.length()} bytes")
    }
    return updateFileName
  }

  /**
   *  Provides the thread safety when multiple threads attempt to load the same plugin.
   *
   *  @return true if tempFile has been moved to cached, false otherwise
   */
  @Synchronized
  private fun moveDownloaded(updateInfo: UpdateInfo, tempFile: File, cached: File): Boolean {
    if (cached.exists()) {
      if (cached.length() >= BROKEN_FILE_THRESHOLD_BYTES) {
        LOG.debug("Update $updateInfo is concurrently loaded by another thread to $cached")
        return false
      }

      if (locksAcquired.getOrDefault(cached, 0) > 0) {
        //we can't delete the cached plugin right now
        return false
      }
      FileUtils.forceDelete(cached)
    }

    FileUtils.moveFile(tempFile, cached)
    return true
  }

  private fun downloadUpdate(updateInfo: UpdateInfo, tempFile: File): File {
    val cachedUpdate = downloadUpdateToTempFileAndGuessFileName(updateInfo, tempFile)
    if (moveDownloaded(updateInfo, tempFile, cachedUpdate)) {
      LOG.debug("Update $updateInfo is saved to $cachedUpdate")
    }
    return cachedUpdate
  }

  private fun downloadUpdateToTempFileAndGuessFileName(updateInfo: UpdateInfo, tempFile: File): File {
    val updateFileName = downloadToTempFile(updateInfo, tempFile)
    return File(downloadDir, updateFileName)
  }

  private fun downloadUpdate(updateInfo: UpdateInfo): File {
    LOG.debug("Downloading update $updateInfo")
    val tempFile = File.createTempFile(TEMP_DOWNLOAD_PREFIX, TEMP_DOWNLOAD_SUFFIX, downloadDir)
    try {
      return downloadUpdate(updateInfo, tempFile)
    } finally {
      if (tempFile.exists()) {
        tempFile.deleteLogged()
      }
    }
  }

  fun getOrDownloadPlugin(updateInfo: UpdateInfo): DownloadPluginResult {
    var pluginFile = getCachedFile(updateInfo)

    if (pluginFile == null || pluginFile.length() < BROKEN_FILE_THRESHOLD_BYTES) {
      try {
        pluginFile = downloadUpdate(updateInfo)
      } catch (e: NotFound404ResponseException) {
        return DownloadPluginResult.NotFound("Plugin $updateInfo is not found the Plugin Repository")
      } catch (e: Exception) {
        val message = "Unable to download update $updateInfo" + (if (e.message != null) " " + e.message else "")
        LOG.info(message, e)
        return DownloadPluginResult.FailedToDownload(message + ": " + e.message)
      }
    }

    val lock = registerLock(pluginFile)

    try {
      val spaceReport = spaceWatcher.getSpaceReport()
      if (spaceReport.availableSpace < spaceReport.lowSpaceThreshold) {
        releaseOldLocksAndDeleteUnusedPlugins()
      }
    } catch (e: Throwable) {
      lock.release()
      throw e
    }

    return DownloadPluginResult.Found(lock)
  }
}