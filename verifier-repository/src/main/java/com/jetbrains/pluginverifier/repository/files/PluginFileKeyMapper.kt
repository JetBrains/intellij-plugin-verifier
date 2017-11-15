package com.jetbrains.pluginverifier.repository.files

import com.google.common.primitives.Ints
import com.jetbrains.pluginverifier.repository.UpdateId
import java.io.File

class PluginFileKeyMapper : FileKeyMapper<UpdateId> {

  private companion object {
    val BROKEN_FILE_THRESHOLD_BYTES = 200
  }

  override fun getFileNameWithoutExtension(key: UpdateId): String =
      key.id.toString()

  override fun getKey(file: File): UpdateId? {
    if (file.isValid()) {
      val id = Ints.tryParse(file.nameWithoutExtension)
      if (id != null) {
        return UpdateId(id)
      }
    }
    return null
  }

  private fun File.isValid() = length() > BROKEN_FILE_THRESHOLD_BYTES
}