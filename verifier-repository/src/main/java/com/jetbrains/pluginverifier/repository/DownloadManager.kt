package com.jetbrains.pluginverifier.repository

import com.google.common.primitives.Ints
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.apache.commons.io.FileUtils
import org.apache.http.annotation.ThreadSafe
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@ThreadSafe
object DownloadManager {

  private data class FileLock(val locked: File, val id: Long, val lockDate: Long) : IFileLock {
    override fun getFile(): File = locked

    override fun release() {
      DownloadManager.releaseLock(this)
    }
  }

  val TEMP_DOWNLOAD_PREFIX = "currentDownload"

  //90% of maximum available space
  val SPACE_THRESHOLD = 0.90

  private val GC_PERIOD_MS: Long = TimeUnit.SECONDS.toMillis(30)

  private val FORGOTTEN_LOCKS_GC_TIMEOUT_MS: Long = TimeUnit.HOURS.toMillis(8)

  init {
    Executors.newSingleThreadScheduledExecutor(
        ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("download-mng-gc-%d")
            .build()
    ).scheduleAtFixedRate({ garbageCollection() }, GC_PERIOD_MS, GC_PERIOD_MS, TimeUnit.MILLISECONDS)
  }

  private var nextId: Long = 0

  private val locksAcquired: MutableMap<File, Int> = hashMapOf()

  private val busyLocks: MutableMap<Long, FileLock> = hashMapOf()

  private val deleteQueue: MutableSet<File> = hashSetOf()

  @Synchronized
  private fun garbageCollection() {
    LOG.debug("It's time for garbage collection!")

    releaseOldLocks()
    if (exceedSpace()) {
      deleteUnusedPlugins()
    }
  }

  private fun deleteUnusedPlugins() {
    RepositoryConfiguration.downloadDir
        .listFiles()!!
        .filterNot { it.name.startsWith(TEMP_DOWNLOAD_PREFIX) }
        .filterNot { it in locksAcquired }
        .filter { Ints.tryParse(it.nameWithoutExtension) != null }
        .sortedBy { Ints.tryParse(it.nameWithoutExtension)!! }
        .forEach { deleteLogged(it) }

    if (exceedSpace()) {
      LOG.error("The available space after garbage collection is not sufficient!")
    }
  }

  private fun exceedSpace(): Boolean {
    val space = FileUtils.sizeOfDirectory(RepositoryConfiguration.downloadDir).toDouble() / FileUtils.ONE_MB
    val threshold = SPACE_THRESHOLD * RepositoryConfiguration.cacheDirMaxSpaceMb
    if (space > threshold) {
      LOG.warn("Download dir occupied space exceeded: $space > $threshold")
    }
    return space > threshold
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
  private fun registerLock(pluginFile: File): IFileLock {
    val id = nextId++
    val cnt = locksAcquired.getOrPut(pluginFile, { 0 })
    locksAcquired.put(pluginFile, cnt + 1)
    val lock = FileLock(pluginFile, id, System.currentTimeMillis())
    busyLocks.put(id, lock)
    return lock
  }

  @Synchronized
  private fun releaseLock(lock: FileLock) {
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
  private fun onResourceRelease(file: File) {
    if (file in deleteQueue) {
      deleteQueue.remove(file)
      LOG.debug("Deleting the plugin file $file")
      deleteLogged(file)
    }
  }

  private fun getCacheFileName(updateId: Int): String = "$updateId.zip"

  /**
   * Performs necessary redirection
   */
  @Throws(IOException::class)
  private fun getFinalUrl(url: URL): URL {
    val connection = url.openConnection() as HttpURLConnection
    connection.instanceFollowRedirects = false

    if (connection.responseCode == HttpURLConnection.HTTP_ACCEPTED) {
      return url
    }

    if (connection.responseCode == HttpURLConnection.HTTP_MOVED_TEMP || connection.responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
      val location = connection.getHeaderField("Location")
      if (location != null) {
        return URL(location)
      }
    }
    return url
  }

  @Throws(MalformedURLException::class)
  private fun getUrlForUpdate(updateId: Int): URL {
    return URL(RepositoryConfiguration.pluginRepositoryUrl + "/plugin/download/?noStatistic=true&updateId=" + updateId)
  }

  @Throws(IOException::class)
  fun getOrLoadUpdate(updateId: Int): IFileLock? {
    var url = getUrlForUpdate(updateId)
    try {
      url = getFinalUrl(url)
    } catch (e: IOException) {
      throw IOException("The repository " + url.host + " problems", e)
    }

    val pluginInCache = File(RepositoryConfiguration.downloadDir, getCacheFileName(updateId))

    if (!pluginInCache.exists() || pluginInCache.length() < BROKEN_ZIP_THRESHOLD) {
      val currentDownload = File.createTempFile(TEMP_DOWNLOAD_PREFIX, ".zip", RepositoryConfiguration.downloadDir)

      LOG.debug("Downloading {} by url {}... ", updateId, url)

      var downloadFail = true
      try {
        FileUtils.copyURLToFile(url, currentDownload)

        if (currentDownload.length() < BROKEN_ZIP_THRESHOLD) {
          LOG.error("Broken zip archive by url {} of file {}", url, currentDownload)
          return null
        }

        LOG.debug("Plugin {} is downloaded", updateId)
        downloadFail = false
      } catch (e: Exception) {
        LOG.error("Error loading plugin " + updateId + " by " + url.toExternalForm(), e)
        return null
      } finally {
        if (downloadFail) {
          deleteLogged(currentDownload)
        }
      }

      //provides the thread safety while multiple threads attempt to load the same plugin.
      synchronized(this) {
        var saveFail = true

        try {
          if (pluginInCache.exists()) {
            //has the other thread downloaded the same plugin already?
            val otherThreadFirst = pluginInCache.length() >= BROKEN_ZIP_THRESHOLD

            //is this file locked now?
            val cantDeleteNow = locksAcquired.getOrElse(pluginInCache, { 0 }) > 0

            if (otherThreadFirst || cantDeleteNow) {
              return registerLock(pluginInCache)
            }

            try {
              //remove the old (possibly broken plugin)
              FileUtils.forceDelete(pluginInCache)
            } catch (e: Exception) {
              LOG.error("Unable to delete cached plugin file " + pluginInCache, e)
              throw e
            }
          }

          try {
            FileUtils.moveFile(currentDownload, pluginInCache)
            saveFail = false
          } catch (e: Exception) {
            LOG.error("Unable to move downloaded plugin file $currentDownload to $pluginInCache", e)
            deleteLogged(pluginInCache)
            throw e
          }
        } finally {
          if (saveFail) {
            deleteLogged(currentDownload)
          }
        }

      }

    }

    val result = registerLock(pluginInCache)

    if (exceedSpace()) {
      garbageCollection()
    }

    return result
  }

  private fun deleteLogged(file: File) {
    LOG.debug("Deleting {}", file)
    if (file.exists()) {
      try {
        FileUtils.forceDelete(file)
      } catch (ce: Exception) {
        LOG.error("Unable to delete file " + file, ce)
      }

    }
  }

  private val LOG: Logger = LoggerFactory.getLogger(DownloadManager::class.java)

  private val BROKEN_ZIP_THRESHOLD = 200
  private val httpDateFormat: DateFormat

  init {
    httpDateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
    httpDateFormat.timeZone = TimeZone.getTimeZone("GMT")
  }

}
