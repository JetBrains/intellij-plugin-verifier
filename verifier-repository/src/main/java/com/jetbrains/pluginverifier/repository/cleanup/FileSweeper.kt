package com.jetbrains.pluginverifier.repository.cleanup

import com.jetbrains.pluginverifier.repository.files.FileRepository

interface FileSweeper<K> {
  fun sweep(fileRepository: FileRepository<K>)
}