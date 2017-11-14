package com.jetbrains.pluginverifier.tests.repository

import com.jetbrains.pluginverifier.repository.cleanup.FileSweeper
import com.jetbrains.pluginverifier.repository.files.FileRepository

class MockFileSweeper : FileSweeper<Int> {
  override fun sweep(fileRepository: FileRepository<Int>) = Unit
}