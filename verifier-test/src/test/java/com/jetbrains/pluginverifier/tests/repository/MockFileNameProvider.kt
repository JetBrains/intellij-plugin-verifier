package com.jetbrains.pluginverifier.tests.repository

import com.jetbrains.pluginverifier.repository.files.FileNameProvider
import java.io.File

class MockFileNameProvider : FileNameProvider<Int> {
  override fun getKey(file: File): Int? = file.nameWithoutExtension.toInt()

  override fun getFileNameWithoutExtension(key: Int): String = key.toString()
}