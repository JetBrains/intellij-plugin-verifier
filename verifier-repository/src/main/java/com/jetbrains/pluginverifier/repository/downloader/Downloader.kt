package com.jetbrains.pluginverifier.repository.downloader

import java.nio.file.Path

/**
 * Downloader is responsible for downloading
 * files and directories with specified keys to a temp directory.
 */
interface Downloader<in K> {
  /**
   * Downloads file or directory by [key] to a file or a directory under [tempDirectory].
   * This file or directory will be moved to the final destination by the caller.
   *
   * @throws InterruptedException if the current thread has been
   * interrupted while downloading the resource.
   */
  @Throws(InterruptedException::class)
  fun download(key: K, tempDirectory: Path): DownloadResult
}