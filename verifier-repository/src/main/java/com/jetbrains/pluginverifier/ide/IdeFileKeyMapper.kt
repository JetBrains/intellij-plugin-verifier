package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.isDirectory
import com.jetbrains.pluginverifier.misc.simpleName
import com.jetbrains.pluginverifier.repository.files.FileKeyMapper
import java.nio.file.Path

class IdeFileKeyMapper : FileKeyMapper<IdeVersion> {

  override fun getFileNameWithoutExtension(key: IdeVersion): String =
      key.asString()

  override fun getKey(file: Path): IdeVersion? = if (file.isDirectory) {
    createIdeVersionSafe(file)?.let { IdeRepository.setProductCodeIfAbsent(it, "IU") }
  } else {
    null
  }

  private fun createIdeVersionSafe(file: Path): IdeVersion? = try {
    IdeVersion.createIdeVersion(file.simpleName)
  } catch (e: IllegalArgumentException) {
    null
  }

}