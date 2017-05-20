package com.jetbrains.pluginverifier.repository

import com.google.common.primitives.Ints
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.jetbrains.pluginverifier.misc.bytesToMegabytes
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.misc.executeSuccessfully
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.apache.commons.io.FileUtils
import org.apache.http.annotation.ThreadSafe
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming
import java.io.File
import java.io.IOException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@ThreadSafe
object DownloadManager {

  private data class FileLockImpl(val locked: File, val id: Long, val lockDate: Long) : FileLock() {
    override fun getFile(): File = locked

    override fun release() {
      DownloadManager.releaseLock(this)
    }
  }

  private val TEMP_DOWNLOAD_PREFIX = "plugin_"

  private val TEMP_DOWNLOAD_SUFFIX = "_download"

  //90% of maximum available space
  private val SPACE_THRESHOLD = 0.90

  private val MAXIMUM_CACHE_SPACE_MB = SPACE_THRESHOLD * RepositoryConfiguration.cacheDirMaxSpaceMb.toLong()

  private val GC_PERIOD_MS: Long = TimeUnit.SECONDS.toMillis(30)

  private val FORGOTTEN_LOCKS_GC_TIMEOUT_MS: Long = TimeUnit.HOURS.toMillis(8)

  private val LOG: Logger = LoggerFactory.getLogger(DownloadManager::class.java)

  private val BROKEN_FILE_THRESHOLD_BYTES = 200

  private val httpDateFormat: DateFormat

  private val JAR_CONTENT_TYPE = MediaType.parse("application/java-archive")

  init {
    httpDateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
    httpDateFormat.timeZone = TimeZone.getTimeZone("GMT")

    Executors.newSingleThreadScheduledExecutor(
        ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("download-mng-gc-%d")
            .build()
    ).scheduleAtFixedRate({ releaseOldLocksAndDeleteUnusedPlugins() }, GC_PERIOD_MS, GC_PERIOD_MS, TimeUnit.MILLISECONDS)
  }

  private var nextId: Long = 0

  private val locksAcquired: MutableMap<File, Int> = hashMapOf()

  private val busyLocks: MutableMap<Long, FileLockImpl> = hashMapOf()

  //it is not used yet
  private val deleteQueue: MutableSet<File> = hashSetOf()

  private fun makeClient(needLog: Boolean): OkHttpClient = OkHttpClient.Builder()
      .connectTimeout(5, TimeUnit.MINUTES)
      .readTimeout(5, TimeUnit.MINUTES)
      .writeTimeout(5, TimeUnit.MINUTES)
      .addInterceptor(HttpLoggingInterceptor().setLevel(if (needLog) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE))
      .build()

  private val downloadApi: DownloadApi = Retrofit.Builder()
      .baseUrl(RepositoryConfiguration.pluginRepositoryUrl)
      .client(makeClient(LOG.isTraceEnabled))
      .build()
      .create(DownloadApi::class.java)

  @Synchronized
  private fun releaseOldLocksAndDeleteUnusedPlugins() {
    LOG.info("It's time to remove unused plugins from cache. Cache usages: ${getCacheSpaceMb()} Mb; Maximum usage: $MAXIMUM_CACHE_SPACE_MB Mb")

    releaseOldLocks()
    if (exceedSpace()) {
      deleteUnusedPlugins()
    }
  }

  private fun deleteUnusedPlugins() {
    val updatesToDelete = RepositoryConfiguration.downloadDir
        .listFiles()!!
        .filterNot { it.name.startsWith(TEMP_DOWNLOAD_PREFIX) && it.name.endsWith(TEMP_DOWNLOAD_SUFFIX) }
        .filterNot { it in locksAcquired }
        .filter { Ints.tryParse(it.nameWithoutExtension) != null }
        .sortedBy { Ints.tryParse(it.nameWithoutExtension)!! }

    LOG.info("Unused updates to be deleted: {}", updatesToDelete.joinToString())

    for (update in updatesToDelete) {
      if (exceedSpace()) {
        update.deleteLogged()
      }
      //already enough space
      break
    }

    if (exceedSpace()) {
      LOG.warn("The available space after garbage collection is not sufficient!")
    }
  }

  private fun exceedSpace(): Boolean {
    val space = getCacheSpaceMb()
    if (space > MAXIMUM_CACHE_SPACE_MB) {
      LOG.warn("Cache directory ${RepositoryConfiguration.downloadDir} occupied to much space: $space > $MAXIMUM_CACHE_SPACE_MB")
      return true
    } else {
      return false
    }
  }

  private fun getCacheSpaceMb() = FileUtils.sizeOfDirectory(RepositoryConfiguration.downloadDir).bytesToMegabytes()

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

  private fun getCachedFile(updateId: Int): File? {
    val asZip = File(RepositoryConfiguration.downloadDir, "$updateId.zip")
    if (asZip.exists() && asZip.length() >= BROKEN_FILE_THRESHOLD_BYTES) {
      return asZip
    }
    val asJar = File(RepositoryConfiguration.downloadDir, "$updateId.jar")
    if (asJar.exists() && asJar.length() >= BROKEN_FILE_THRESHOLD_BYTES) {
      return asJar
    }
    return null
  }

  private fun doDownload(updateId: Int, tempFile: File): String {
    val response = downloadApi.downloadFile(updateId).executeSuccessfully()
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

  private fun downloadToTempFile(updateId: Int, tempFile: File): String {
    val updateFileName = doDownload(updateId, tempFile)
    if (tempFile.length() < BROKEN_FILE_THRESHOLD_BYTES) {
      throw IOException("Too small update #$updateId size: ${tempFile.length()} bytes")
    }
    return updateFileName
  }

  /**
   *  Provides the thread safety when multiple threads attempt to load the same plugin.
   *
   *  @return true if tempFile has been moved to cached, false otherwise
   */
  @Synchronized
  private fun moveDownloaded(tempFile: File, cached: File): Boolean {
    if (cached.exists()) {
      if (cached.length() >= BROKEN_FILE_THRESHOLD_BYTES) {
        //the other thread has already downloaded the plugin
        return false
      }

      if (locksAcquired.getOrElse(cached, { 0 }) > 0) {
        //we can't delete the cached plugin right now
        return false
      }
      FileUtils.forceDelete(cached)
    }

    FileUtils.moveFile(tempFile, cached)
    return true
  }

  private fun downloadFile(updateId: Int): File {
    LOG.debug("Downloading update #$updateId")
    val tempFile = File.createTempFile(TEMP_DOWNLOAD_PREFIX, TEMP_DOWNLOAD_SUFFIX, RepositoryConfiguration.downloadDir)
    val updateFileName = downloadToTempFile(updateId, tempFile)
    val cachedUpdate = File(RepositoryConfiguration.downloadDir, updateFileName)
    if (moveDownloaded(tempFile, cachedUpdate)) {
      LOG.debug("Update #$updateId is saved to $cachedUpdate")
    } else {
      LOG.debug("Update #$updateId is concurrently loaded by another thread to $cachedUpdate")
      tempFile.deleteLogged()
    }
    return cachedUpdate
  }

  fun getOrLoadUpdate(updateId: Int): FileLock? {
    var pluginFile = getCachedFile(updateId)

    if (pluginFile == null || pluginFile.length() < BROKEN_FILE_THRESHOLD_BYTES) {
      try {
        pluginFile = downloadFile(updateId)
      } catch (e: Exception) {
        LOG.info("Unable to download update #$updateId", e)
        return null
      }
    }

    val lock = registerLock(pluginFile)

    try {
      if (exceedSpace()) {
        releaseOldLocksAndDeleteUnusedPlugins()
      }
    } catch (e: Throwable) {
      lock.release()
      throw e
    }

    return lock
  }
}

private interface DownloadApi {

  @GET("/plugin/download/?noStatistic=true")
  @Streaming
  fun downloadFile(@Query("updateId") updateId: Int): Call<ResponseBody>

}
