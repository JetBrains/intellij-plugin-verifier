package com.jetbrains.pluginverifier

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.replaceInvalidFileNameCharacters
import com.jetbrains.pluginverifier.repository.PluginInfo
import java.io.Serializable
import java.nio.file.Path

sealed class VerificationTarget : Serializable {

  data class Ide(val ideVersion: IdeVersion) : VerificationTarget() {
    override fun toString() = ideVersion.toString()

    companion object {
      private const val serialVersionUID = 0L
    }
  }

  data class Plugin(val plugin: PluginInfo) : VerificationTarget() {
    override fun toString() = plugin.toString()

    companion object {
      private const val serialVersionUID = 0L
    }
  }

  fun getReportDirectory(baseDirectory: Path): Path = when (this) {
    is VerificationTarget.Ide -> baseDirectory.resolve(ideVersion.asString().replaceInvalidFileNameCharacters())
    is VerificationTarget.Plugin -> baseDirectory.resolve("${plugin.pluginId} ${plugin.version}".replaceInvalidFileNameCharacters())
  }

}