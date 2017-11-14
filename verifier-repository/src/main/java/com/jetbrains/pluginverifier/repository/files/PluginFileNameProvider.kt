package com.jetbrains.pluginverifier.repository.files

import com.google.common.primitives.Ints
import com.jetbrains.pluginverifier.repository.UpdateId
import java.io.File

class PluginFileNameProvider : FileNameProvider<UpdateId> {
  override fun getFileNameWithoutExtension(key: UpdateId): String =
      key.id.toString()

  override fun getKey(file: File): UpdateId? {
    val id = Ints.tryParse(file.nameWithoutExtension)
    if (id != null) {
      return UpdateId(id)
    }
    return null
  }
}