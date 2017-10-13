package com.jetbrains.pluginverifier.results

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginInfo

data class Result(val plugin: PluginInfo,
                  val ideVersion: IdeVersion,
                  val verdict: Verdict) {
  override fun toString(): String = "Plugin $plugin and #$ideVersion: $verdict"
}