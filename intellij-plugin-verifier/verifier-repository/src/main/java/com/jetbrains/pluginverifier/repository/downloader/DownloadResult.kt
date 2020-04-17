/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.downloader

import java.nio.file.Path

/**
 * Represents possible [download] [Downloader.download] outcomes.
 */
sealed class DownloadResult {
  /**
   * File or directory is successfully downloaded to [downloadedFileOrDirectory].
   * Whether it is a file or directory can be detected in [isDirectory].
   * If file, not directory, is downloaded, the [extension] specifies its extension.
   */
  data class Downloaded(val downloadedFileOrDirectory: Path, val extension: String, val isDirectory: Boolean) : DownloadResult()

  /**
   * Downloading is failed because the specified resource is not found.
   * It may be caused by HTTP 404 error if a URL is no more valid.
   * The actual reason can be observed in [reason].
   */
  data class NotFound(val reason: String) : DownloadResult()

  /**
   * Resource was not downloaded due to network or other failure,
   * reason of which is the [reason] and a thrown exception is [error].
   */
  data class FailedToDownload(val reason: String, val error: Exception) : DownloadResult()
}