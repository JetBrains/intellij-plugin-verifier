package com.jetbrains.pluginverifier.tests.repository

import com.jetbrains.pluginverifier.repository.files.FileKeyMapper
import java.io.File

class IntFileKeyMapper : FileKeyMapper<Int> {

  override fun getKey(file: File): Int? {
    return file.nameWithoutExtension.toIntOrNull()
  }

  override fun getFileNameWithoutExtension(key: Int): String = key.toString()
}