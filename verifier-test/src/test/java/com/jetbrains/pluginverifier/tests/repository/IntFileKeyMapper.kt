package com.jetbrains.pluginverifier.tests.repository

import com.jetbrains.pluginverifier.misc.nameWithoutExtension
import com.jetbrains.pluginverifier.repository.files.FileKeyMapper
import java.nio.file.Path

class IntFileKeyMapper : FileKeyMapper<Int> {

  override fun getKey(file: Path): Int? = file.nameWithoutExtension.toIntOrNull()

  override fun getFileNameWithoutExtension(key: Int): String = key.toString()
}