package com.jetbrains.pluginverifier.repository.files

import com.google.common.primitives.Ints
import com.jetbrains.pluginverifier.misc.nameWithoutExtension
import com.jetbrains.pluginverifier.repository.UpdateId
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.cleanup.fileSize
import java.nio.file.Path

class PluginFileKeyMapper : FileKeyMapper<UpdateId> {

  companion object {
    private val BROKEN_FILE_THRESHOLD = SpaceAmount.ONE_BYTE * 200

    private fun Path.isValid() = fileSize > BROKEN_FILE_THRESHOLD

    fun getUpdateIdByFile(file: Path): UpdateId? {
      if (file.isValid()) {
        val id = Ints.tryParse(file.nameWithoutExtension)
        if (id != null) {
          return UpdateId(id)
        }
      }
      return null
    }
  }

  override fun getFileNameWithoutExtension(key: UpdateId): String =
      key.id.toString()

}