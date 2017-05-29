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

  val LOG: Logger = LoggerFactory.getLogger(DownloadManager::class.java)

  private val TEMP_DOWNLOAD_PREFIX = "plugin_"

  private val TEMP_DOWNLOAD_SUFFIX = "_download"

  private val FORGOTTEN_LOCKS_GC_TIMEOUT_MS: Long = TimeUnit.HOURS.toMillis(8)

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
    ).scheduleAtFixedRate({ releaseOldLocksAndDeleteUnusedPlugins() }, 5, 30, TimeUnit.SECONDS)
  }

  private var nextId: Long = 0

  private val locksAcquired: MutableMap<File, Int> = hashMapOf()

  private val busyLocks: MutableMap<Long, FileLockImpl> = hashMapOf()

  //it is not used yet
  private val deleteQueue: MutableSet<File> = hashSetOf()

  private val spaceWatcher = FreeDiskSpaceWatcher(RepositoryConfiguration.downloadDir, RepositoryConfiguration.downloadDirMaxSpace)

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
    LOG.info("It's time to remove unused plugins from cache. Cache usages: ${spaceWatcher.getSpaceUsageMb()} Mb; " +
        "Estimated available space: ${spaceWatcher.estimateAvailableSpace()} Mb")

    releaseOldLocks()
    if (spaceWatcher.isLowSpace()) {
      deleteUnusedPlugins()
    }
  }

  private fun deleteUnusedPlugins() {
    val updatesToDelete = RepositoryConfiguration.downloadDir
        .listFiles()!!
        .filterNot { it.name.startsWith(TEMP_DOWNLOAD_PREFIX) && it.name.endsWith(TEMP_DOWNLOAD_SUFFIX) }
        .filterNot { it in locksAcquired }
        .filter { Ints.tryParse(it.nameWithoutExtension) != null }
        .sortedByDescending { it.length() }

    LOG.info("Unused updates to be deleted: [{}]", updatesToDelete.joinToString())

    for (update in updatesToDelete) {
      if (spaceWatcher.isEnoughSpace()) {
        LOG.debug("Enough space after cleanup: ${spaceWatcher.estimateAvailableSpace()} Mb")
        break
      }
      LOG.debug("Deleting unused update $update with size ${update.length().bytesToMegabytes()} Mb")
      update.deleteLogged()
    }

    if (spaceWatcher.isLowSpace()) {
      LOG.warn("Available space after cleanup is not sufficient!: ${spaceWatcher.estimateAvailableSpace()} Mb")
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

  private fun doDownloadAndGuessFileName(updateInfo: UpdateInfo, tempFile: File): String {
    val updateId = updateInfo.updateId
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
    val cachedUpdate = downloadUpdateToTempFileAndReturnDestinationFile(updateInfo, tempFile)
    if (moveDownloaded(updateInfo, tempFile, cachedUpdate)) {
      LOG.debug("Update $updateInfo is saved to $cachedUpdate")
    }
    return cachedUpdate
  }

  private fun downloadUpdateToTempFileAndReturnDestinationFile(updateInfo: UpdateInfo, tempFile: File): File {
    val updateFileName = downloadToTempFile(updateInfo, tempFile)
    return File(RepositoryConfiguration.downloadDir, updateFileName)
  }

  private fun downloadUpdate(updateInfo: UpdateInfo): File {
    LOG.debug("Downloading update $updateInfo")
    val tempFile = File.createTempFile(TEMP_DOWNLOAD_PREFIX, TEMP_DOWNLOAD_SUFFIX, RepositoryConfiguration.downloadDir)
    try {
      return downloadUpdate(updateInfo, tempFile)
    } finally {
      if (tempFile.exists()) {
        tempFile.deleteLogged()
      }
    }
  }

  fun getOrLoadUpdate(updateInfo: UpdateInfo): FileLock? {
    var pluginFile = getCachedFile(updateInfo)

    if (pluginFile == null || pluginFile.length() < BROKEN_FILE_THRESHOLD_BYTES) {
      try {
        pluginFile = downloadUpdate(updateInfo)
      } catch (e: Exception) {
        LOG.info("Unable to download update $updateInfo", e)
        return null
      }
    }

    val lock = registerLock(pluginFile)

    try {
      if (spaceWatcher.isLowSpace()) {
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
