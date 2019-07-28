package com.jetbrains.pluginverifier

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.PluginIdAndVersion
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginInfo

sealed class PluginVerificationTarget {

  data class IDE(private val ide: Ide) : PluginVerificationTarget() {
    val ideVersion: IdeVersion
      get() = ide.version

    val incompatiblePlugins: Set<PluginIdAndVersion>
      get() = ide.incompatiblePlugins

    override fun toString() = ideVersion.asString()
  }

  data class Plugin(val plugin: PluginInfo) : PluginVerificationTarget() {
    override fun toString() = plugin.toString()
  }

}