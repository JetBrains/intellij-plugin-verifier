package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.files.FileKeyMapper
import java.io.File

class IdeFileKeyMapper : FileKeyMapper<IdeVersion> {
  override val directoriesStored: Boolean = true

  override fun getFileNameWithoutExtension(key: IdeVersion): String =
      key.asStringWithoutProductCode()

  override fun getKey(file: File): IdeVersion? {
    if (file.isDirectory) {
      return createIdeVersionSafe(file)
    }
    return null
  }

  private fun createIdeVersionSafe(file: File): IdeVersion? = try {
    IdeVersion.createIdeVersion(file.name)
  } catch (e: IllegalArgumentException) {
    null
  }

}