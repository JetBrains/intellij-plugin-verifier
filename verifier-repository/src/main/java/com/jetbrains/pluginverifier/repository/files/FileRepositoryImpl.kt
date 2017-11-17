package com.jetbrains.pluginverifier.repository.files

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.jetbrains.pluginverifier.misc.*
import com.jetbrains.pluginverifier.repository.cleanup.SweepInfo
import com.jetbrains.pluginverifier.repository.cleanup.SweepPolicy
import com.jetbrains.pluginverifier.repository.cleanup.UsageStatistic
import com.jetbrains.pluginverifier.repository.downloader.DownloadResult
import com.jetbrains.pluginverifier.repository.downloader.Downloader
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.time.Clock
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit


class FileRepositoryImpl<K>(private val repositoryDir: File,
                            private val downloader: Downloader<K>,
                            private val fileKeyMapper: FileKeyMapper<K>,
                            private val sweepPolicy: SweepPolicy<K>,
                            private val clock: Clock = Clock.systemUTC()) : FileRepository<K> {

  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(FileRepositoryImpl::class.java)

    val LOCK_TIME_TO_LIVE_DURATION: Duration = Duration.of(1, ChronoUnit.HOURS)
  }

  private data class FileInfo(val file: File, val size: Long)

  private data class RepositoryState<K>(var totalSpaceUsage: Long = 0,
                                        val files: MutableMap<K, FileInfo> = hashMapOf()) {
    fun addFile(key: K, file: File) {
      assert(key !in files)
      LOG.debug("Adding file by $key: $file")
      val fileSize = file.length()
      totalSpaceUsage += fileSize
      files[key] = FileInfo(file, fileSize)
    }

    fun getAllKeys() = files.keys

    fun has(key: K) = key in files

    fun get(key: K) = files[key]

    fun deleteFile(key: K) {
      assert(key in files)
      val fileInfo = files[key]!!
      LOG.debug("Deleting file by $key: $fileInfo")
      totalSpaceUsage -= fileInfo.size
      files.remove(key)
    }
  }

  private val repositoryState = RepositoryState<K>()

  private var nextLockId: Long = 0

  private val key2Locks = hashMapOf<K, MutableSet<FileLock>>()

  private val deleteQueue = hashSetOf<K>()

  private val downloading = hashMapOf<K, FutureTask<DownloadResult>>()

  private val statistics = hashMapOf<K, UsageStatistic>()

  init {
    repositoryDir.createDir()
    readInitiallyAvailableFiles()
    runForgottenLocksInspector()
  }

  private fun runForgottenLocksInspector() {
    Executors.newSingleThreadScheduledExecutor(
        ThreadFactoryBuilder()
            .setDaemon(true)
            .build()
    ).scheduleAtFixedRate({ detectForgottenLocks() }, 1, 60, TimeUnit.MINUTES)
  }

  private fun readInitiallyAvailableFiles() {
    val existingFiles = repositoryDir.listFiles() ?: throw IOException("Unable to read directory content: $repositoryDir")
    for (file in existingFiles) {
      val key = fileKeyMapper.getKey(file)
      if (key != null) {
        repositoryState.addFile(key, file)
      }
    }
  }

  @Synchronized
  override fun <R> lockAndAccess(block: () -> R): R = block()

  @Synchronized
  override fun getAllExistingKeys(): Set<K> = repositoryState.getAllKeys()

  @Synchronized
  override fun has(key: K): Boolean = repositoryState.has(key)

  @Synchronized
  override fun remove(key: K): Boolean {
    if (!repositoryState.has(key)) {
      return false
    }
    val isLocked = isLockedKey(key)
    return if (isLocked) {
      LOG.debug("File by $key is locked. Putting it into deletion queue.")
      deleteQueue.add(key)
      false
    } else {
      repositoryState.deleteFile(key)
      true
    }
  }

  @Synchronized
  private fun isLockedKey(key: K) =
      key2Locks[key].orEmpty().isNotEmpty()

  @Synchronized
  private fun registerLock(key: K, isFake: Boolean): FileLockImpl<K> {
    val fileInfo = if (isFake) {
      FileInfo(File("Indicates that the file is being downloaded. It is not accessed ever."), 0)
    } else {
      assert(repositoryState.has(key))
      repositoryState.get(key)!!
    }
    val lockTime = clock.instant()
    val lock = FileLockImpl(fileInfo.file, lockTime, key, nextLockId++, this)
    key2Locks.getOrPut(key, { hashSetOf() }).add(lock)
    val keyUsageStatistic = statistics.getOrPut(key, { UsageStatistic(lockTime, 0) })
    keyUsageStatistic.timesAccessed++
    return lock
  }

  @Synchronized
  internal fun releaseLock(lock: FileLockImpl<K>) {
    val key = lock.key
    val fileLocks = key2Locks[key]
    if (fileLocks != null) {
      fileLocks.remove(lock)
      if (fileLocks.isEmpty()) {
        key2Locks.remove(key)
        statistics.remove(key)
        onResourceRelease(key)
      }
    }
  }

  @Synchronized
  private fun onResourceRelease(key: K) {
    assert(!isLockedKey(key))
    if (key in deleteQueue) {
      deleteQueue.remove(key)
      if (repositoryState.has(key)) {
        repositoryState.deleteFile(key)
      }
    }
  }

  private val downloadDirectory by lazy {
    File(repositoryDir, "downloads").createDir()
  }

  private fun fetchFile(key: K): FileRepositoryResult {
    val (downloadTask, runInCurrentThread, waitingLock) = synchronized(this) {
      val waitingLock = registerLock(key, true)
      val existingTask = downloading[key]
      if (existingTask != null) {
        Triple(existingTask, false, waitingLock)
      } else {
        val downloadTask = FutureTask { doDownload(key) }
        downloading[key] = downloadTask
        Triple(downloadTask, true, waitingLock)
      }
    }

    //Run the downloading task if the current thread has initialized it.
    if (runInCurrentThread) {
      downloadTask.run()
    }

    try {
      val downloadResult = downloadTask.get()
      return downloadResult.toFileRepositoryResult(key)
    } finally {
      waitingLock.release()
      if (runInCurrentThread) {
        synchronized(this) {
          downloading.remove(key)
        }
      }
    }
  }

  private fun doDownload(key: K): DownloadResult {
    val tempFileOrDirectory = createTempFileOrDirectory(key)
    try {
      val downloadResult = downloader.download(key, tempFileOrDirectory)
      if (downloadResult is DownloadResult.Downloaded) {
        val finalFile = saveTempFileToFinalFile(key, tempFileOrDirectory, downloadResult.extension)
        repositoryState.addFile(key, finalFile)
        return DownloadResult.Downloaded(downloadResult.extension)
      }
      return downloadResult
    } catch (e: Throwable) {
      tempFileOrDirectory.deleteLogged()
      throw e
    }
  }

  private fun createTempFileOrDirectory(key: K): File {
    val tempPrefix = "download-" + getFileName(key, "") + "-"
    val tempFileOrDir = if (fileKeyMapper.directoriesStored) {
      Files.createTempDirectory(downloadDirectory.toPath(), tempPrefix)
    } else {
      Files.createTempFile(downloadDirectory.toPath(), tempPrefix, "")
    }
    return tempFileOrDir.toFile()
  }

  private fun saveTempFileToFinalFile(key: K, tempFile: File, extension: String): File {
    val finalFile = getFileForKey(key, extension)
    assert(!finalFile.exists())
    FileUtils.moveFile(tempFile, finalFile)
    return finalFile
  }

  private fun DownloadResult.toFileRepositoryResult(key: K) = when (this) {
    is DownloadResult.Downloaded -> FileRepositoryResult.Found(registerLock(key, false))
    is DownloadResult.NotFound -> FileRepositoryResult.NotFound(reason)
    is DownloadResult.FailedToDownload -> FileRepositoryResult.Failed(reason, error)
  }

  private fun getFileForKey(key: K, extension: String): File {
    val finalFileName = getFileName(key, extension)
    return File(repositoryDir, finalFileName)
  }

  private fun getFileName(key: K, extension: String): String {
    val nameWithoutExtension = fileKeyMapper.getFileNameWithoutExtension(key)
    val fullName = nameWithoutExtension + if (extension.isEmpty()) "" else "." + extension
    return fullName.replaceInvalidFileNameCharacters()
  }

  @Synchronized
  private fun lockFileIfExists(key: K): FileLockImpl<K>? {
    if (repositoryState.has(key)) {
      return registerLock(key, false)
    }
    return null
  }

  private fun detectForgottenLocks() {
    for ((key, locks) in key2Locks) {
      for (lock in locks) {
        val now = clock.instant()
        val lockTime = lock.lockTime
        val maxUnlockTime = lockTime.plus(LOCK_TIME_TO_LIVE_DURATION)
        val isForgotten = now.isAfter(maxUnlockTime)
        if (isForgotten) {
          LOG.warn("Forgotten lock found for $key on ${lock.file}; lock date = $lockTime")
        }
      }
    }
  }

  @Synchronized
  override fun sweep() {
    val availableFiles = repositoryState
        .files
        .map { (key, fileInfo) ->
          AvailableFile(key, fileInfo.file, fileInfo.size, statistics[key]!!)
        }

    val sweepInfo = SweepInfo(repositoryState.totalSpaceUsage, availableFiles)
    val filesForDeletion = sweepPolicy.selectFilesForDeletion(sweepInfo)

    if (filesForDeletion.isNotEmpty()) {
      val deletionsSize = filesForDeletion.map { it.size }.sum()
      LOG.info("It's time to remove unused files.\n" +
          "Space usage: ${repositoryState.totalSpaceUsage.bytesToMegabytes()} Mb;\n" +
          "${filesForDeletion.size} " + "file".pluralize(filesForDeletion.size) +
          " will be removed having total size " + deletionsSize.bytesToMegabytes() + "Mb"
      )
      for (availableFile in filesForDeletion) {
        remove(availableFile.key)
      }
    }

  }

  /**
   * Searches the file by [key] in the local cache. If it isn't found there,
   * downloads the file using [downloader].
   *
   * The possible results are represented as subclasses of [FileRepositoryResult].
   * If the file is found locally or successfully downloaded, the file lock is registered
   * for the file so it will be protected against deletions by other threads.
   *
   * This method is thread safe. In case several threads attempt to get the same file, only one
   * of them will download it while others will wait for the first to complete.
   */
  override fun get(key: K): FileRepositoryResult {
    val lockedFile = lockFileIfExists(key)
    if (lockedFile != null) {
      return FileRepositoryResult.Found(lockedFile)
    }
    return try {
      fetchFile(key)
    } finally {
      sweep()
    }
  }

}