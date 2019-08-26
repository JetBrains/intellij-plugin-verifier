package com.jetbrains.pluginverifier.resources

import com.jetbrains.pluginverifier.repository.downloader.DownloadResult
import com.jetbrains.pluginverifier.repository.downloader.Downloader
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class OnlyOneDownloadAtTimeDownloader : Downloader<Int> {
  val errors: MutableList<Throwable> = Collections.synchronizedList(arrayListOf<Throwable>())

  private val downloading = ConcurrentHashMap<Int, Thread>()

  private val downloadResult = DownloadResult.NotFound("Not found")

  override fun download(key: Int, tempDirectory: Path): DownloadResult {
    val thread = downloading[key]
    if (thread != null) {
      errors.add(AssertionError("Key $key is already being downloaded by $thread; current thread = " + Thread.currentThread()))
      return downloadResult
    }
    downloading[key] = Thread.currentThread()
    try {
      doDownload()
    } finally {
      downloading.remove(key)
    }
    return downloadResult
  }

  @Synchronized
  private fun doDownload() {
    Thread.sleep(1000)
  }
}