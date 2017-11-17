package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.files.FileKeyMapper
import java.io.File

class IdeFileKeyMapper : FileKeyMapper<IdeVersion> {

  override fun getFileNameWithoutExtension(key: IdeVersion): String =
      key.asString()

  override fun getKey(file: File): IdeVersion? = if (file.isDirectory) {
    createIdeVersionSafe(file)?.let { IdeRepository.setProductCodeIfAbsent(it, "IU") }
  } else {
    null
  }

  private fun createIdeVersionSafe(file: File): IdeVersion? = try {
    IdeVersion.createIdeVersion(file.name)
  } catch (e: IllegalArgumentException) {
    null
  }

}