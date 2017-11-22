package com.jetbrains.pluginverifier.repository.files

import com.google.common.primitives.Ints
import com.jetbrains.pluginverifier.misc.nameWithoutExtension
import com.jetbrains.pluginverifier.repository.UpdateId
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.cleanup.fileSize
import java.nio.file.Path

class PluginFileKeyMapper : FileKeyMapper<UpdateId> {
  private companion object {
    val BROKEN_FILE_THRESHOLD = SpaceAmount.ONE_BYTE * 200
  }

  override fun getFileNameWithoutExtension(key: UpdateId): String =
      key.id.toString()

  override fun getKey(file: Path): UpdateId? {
    if (file.isValid()) {
      val id = Ints.tryParse(file.nameWithoutExtension)
      if (id != null) {
        return UpdateId(id)
      }
    }
    return null
  }

  private fun Path.isValid() = fileSize > BROKEN_FILE_THRESHOLD
}