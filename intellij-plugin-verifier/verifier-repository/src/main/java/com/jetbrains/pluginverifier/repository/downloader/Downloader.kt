/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

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