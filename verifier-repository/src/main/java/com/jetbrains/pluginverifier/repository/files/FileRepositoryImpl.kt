package com.jetbrains.pluginverifier.repository.files

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.jetbrains.pluginverifier.misc.createDir
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.repository.FileLock
import com.jetbrains.pluginverifier.repository.cleanup.FileSweeper
import com.jetbrains.pluginverifier.repository.downloader.DownloadResult
import com.jetbrains.pluginverifier.repository.downloader.Downloader
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask
import java.util.concurrent.atomic.AtomicInteger


class FileRepositoryImpl<K>(private val repositoryDir: File,
                            private val downloader: Downloader<K>,
                            private val fileKeyMapper: FileKeyMapper<K>,
                            private val fileSweeper: FileSweeper<K>) : FileRepository<K> {

  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(FileRepositoryImpl::class.java)
  }

  private var nextId: Long = 0

  private val key2Locks = hashMapOf<K, MutableSet<FileLockImpl<K>>>()

  private val key2File = hashMapOf<K, File>()

  private val deleteQueue = hashSetOf<K>()

  private val downloading = hashMapOf<K, Pair<FutureTask<DownloadResult>, AtomicInteger>>()

  private val sweeperExecutor = Executors.newSingleThreadExecutor(
      ThreadFactoryBuilder()
          .setDaemon(true)
          .build()
  )

  init {
    repositoryDir.createDir()
    readInitiallyAvailableFiles()
  }

  private fun readInitiallyAvailableFiles() {
    val existingFiles = repositoryDir.listFiles() ?: throw IOException("Unable to read directory content: $repositoryDir")
    for (file in existingFiles) {
      val key = fileKeyMapper.getKey(file)
        if (key != null) {
          key2File[key] = file
      }
    }
  }

  @Synchronized
  override fun has(key: K): Boolean = key in key2File

  @Synchronized
  override fun remove(key: K): Boolean {
    val file = key2File[key] ?: return false
    val isLocked = isLockedKey(key)
    return if (isLocked) {
      LOG.debug("File by $key is already locked: $file. Putting it into deletion queue.")
      deleteQueue.add(key)
      false
    } else {
      LOG.debug("Removing unlocked file by $key: $file")
      file.deleteLogged()
      true
    }
  }

  @Synchronized
  private fun isLockedKey(key: K) =
      key2Locks[key].orEmpty().isNotEmpty() || key in downloading

  @Synchronized
  override fun getAvailableFiles() = key2File.map { (key, file) ->
    val fileLocks = key2Locks[key].orEmpty()
    AvailableFile(key, file, file.length(), fileLocks)
  }

  @Synchronized
  private fun registerLock(key: K): FileLockImpl<K> {
    val file = key2File[key]!!
    val lock = FileLockImpl(file, System.currentTimeMillis(), key, nextId++, this)
    key2Locks.getOrPut(key, { hashSetOf() }).add(lock)
    return lock
  }

  @Synchronized
  fun releaseLock(lock: FileLockImpl<K>) {
    val key = lock.key
    val fileLocks = key2Locks[key]
    if (fileLocks != null) {
      assert(key !in downloading)
      fileLocks.remove(lock)
      if (fileLocks.isEmpty()) {
        key2Locks.remove(key)
        onResourceRelease(key)
      }
    }
  }

  @Synchronized
  private fun onResourceRelease(key: K) {
    assert(!isLockedKey(key))
    if (key in deleteQueue) {
      deleteQueue.remove(key)
      val file = key2File.remove(key)
      if (file != null) {
        LOG.debug("Deleting file by $key: $file")
        file.deleteLogged()
      }
    }
  }

  /**
   *  Provides the thread safety when multiple threads attempt to load the same file.
   *
   *  @return true if [downloaded] has been moved to [finalFile], false otherwise
   */
  @Synchronized
  private fun moveDownloaded(downloaded: File, finalFile: File): Boolean {
    if (finalFile.exists()) {
      val isValid = fileKeyMapper.getKey(finalFile) != null
      if (isValid) {
        return false
      } else {
        finalFile.deleteLogged()
      }
    }
    FileUtils.moveFile(downloaded, finalFile)
    return true
  }

  private val downloadDirectory by lazy {
    File(repositoryDir, "downloads").createDir()
  }

  private fun fetchFile(key: K): FileRepositoryResult {
    val (downloadTask, runInCurrentThread) = synchronized(this) {
      val keyDownloading = downloading[key]
      if (keyDownloading != null) {
        keyDownloading.second.incrementAndGet()
        keyDownloading.first to false
      } else {
        val task = FutureTask { downloader.download(downloadDirectory, key) }
        this.downloading[key] = task to AtomicInteger(1)
        task to true
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
      synchronized(this) {
        if (downloading[key]!!.second.decrementAndGet() == 0) {
          downloading.remove(key)
        }
      }
    }
  }

  private fun DownloadResult.toFileRepositoryResult(key: K): FileRepositoryResult = when (this) {
    is DownloadResult.Downloaded -> {
      val finalFile = getFinalFile(key, extension)
      val fileLock = synchronized(this) {
        if (file.exists()) {
          saveDownloadedToFinalFile(key, file, finalFile)
        } else {
          assert(has(key))
          registerLock(key)
        }
      }
      FileRepositoryResult.Found(fileLock)
    }
    is DownloadResult.NotFound -> FileRepositoryResult.NotFound(reason)
    is DownloadResult.FailedToDownload -> FileRepositoryResult.Failed(reason, error)
  }

  private fun getFinalFile(key: K, extension: String): File {
    val finalFileName = fileKeyMapper.getFileNameWithoutExtension(key) + if (extension.isEmpty()) "" else "." + extension
    return File(repositoryDir, finalFileName)
  }

  @Synchronized
  private fun saveDownloadedToFinalFile(key: K, downloadedFile: File, finalFile: File): FileLock {
    assert(downloadedFile.exists())
    if (moveDownloaded(downloadedFile, finalFile)) {
      LOG.debug("$key is saved into $finalFile")
    } else {
      LOG.debug("Another thread has downloaded $key into $finalFile")
    }
    registerFileByKey(key, finalFile)
    return registerLock(key)
  }

  private fun registerFileByKey(key: K, finalFile: File) {
    key2File[key] = finalFile
  }

  @Synchronized
  private fun lockFileIfExists(key: K): FileLockImpl<K>? {
    if (key in key2File) {
      return registerLock(key)
    }
    return null
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
      sweeperExecutor.submit { fileSweeper.sweep(this) }
    }
  }

}