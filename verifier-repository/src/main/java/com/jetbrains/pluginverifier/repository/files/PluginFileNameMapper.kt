package com.jetbrains.pluginverifier.repository.files

import com.google.common.primitives.Ints
import com.jetbrains.pluginverifier.misc.nameWithoutExtension
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.cleanup.fileSize
import java.nio.file.Path

class PluginFileNameMapper : FileNameMapper<Int> {

  companion object {
    private val BROKEN_FILE_THRESHOLD = SpaceAmount.ONE_BYTE * 200

    private fun Path.isValid() = fileSize > BROKEN_FILE_THRESHOLD

    fun getUpdateIdByFile(file: Path): Int? {
      if (file.isValid()) {
        return Ints.tryParse(file.nameWithoutExtension)
      }
      return null
    }
  }

  override fun getFileNameWithoutExtension(key: Int): String =
      key.toString()

}