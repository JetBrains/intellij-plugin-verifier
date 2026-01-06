/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

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
    val current = Thread.currentThread()
    val thread = downloading.putIfAbsent(key, current)
    if (thread != null) {
      errors.add(AssertionError("Key $key is already being downloaded by $thread; current thread = $current"))
      return downloadResult
    }
    try {
      doDownload()
    } finally {
      downloading.remove(key, current)
    }
    return downloadResult
  }

  private fun doDownload() {
    Thread.sleep(1000)
  }
}