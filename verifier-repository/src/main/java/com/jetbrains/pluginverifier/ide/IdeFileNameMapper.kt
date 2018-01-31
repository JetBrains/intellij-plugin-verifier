package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.isDirectory
import com.jetbrains.pluginverifier.misc.simpleName
import com.jetbrains.pluginverifier.repository.files.FileNameMapper
import java.nio.file.Path

class IdeFileNameMapper : FileNameMapper<IdeVersion> {

  companion object {
    fun getIdeVersionByFile(file: Path) =
        if (file.isDirectory) {
          IdeVersion.createIdeVersionIfValid(file.simpleName)
              ?.let { IdeRepository.setProductCodeIfAbsent(it, "IU") }
        } else {
          null
        }

  }

  override fun getFileNameWithoutExtension(key: IdeVersion): String = key.asString()

}