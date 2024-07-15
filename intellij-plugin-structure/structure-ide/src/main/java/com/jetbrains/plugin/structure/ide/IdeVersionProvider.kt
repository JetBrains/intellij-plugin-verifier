package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.readText
import com.jetbrains.plugin.structure.ide.IdeVersionResolution.Failed
import com.jetbrains.plugin.structure.ide.IdeVersionResolution.Found
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.nio.file.Path

interface IdeVersionProvider {
  fun readIdeVersion(idePath: Path): IdeVersionResolution
}

sealed class IdeVersionResolution {
  class Found(val ideVersion: IdeVersion, val origin: String) : IdeVersionResolution()
  class NotFound(val locations: List<Path>) : IdeVersionResolution()
  class Failed(e: Throwable) : IdeVersionResolution()
}

class BuildTxtIdeVersionProvider : IdeVersionProvider{
  override fun readIdeVersion(idePath: Path): IdeVersionResolution {
    val locations = listOf(
      idePath.resolve("build.txt"),
      idePath.resolve("Resources").resolve("build.txt"),
      idePath.resolve("community").resolve("build.txt"),
      idePath.resolve("ultimate").resolve("community").resolve("build.txt")
    )
    val buildTxtFile = locations.find { it.exists() }
      ?: return IdeVersionResolution.NotFound(locations)
    return try {
      Found(readBuildNumber(buildTxtFile), buildTxtFile.toString())
    } catch (e: IllegalArgumentException) {
      Failed(e)
    }
  }
  private fun readBuildNumber(versionFile: Path): IdeVersion {
    val buildNumberString = versionFile.readText().trim()
    return IdeVersion.createIdeVersion(buildNumberString)
  }
}