package com.jetbrains.pluginverifier.results

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.problems.Problem

data class Result(val plugin: PluginInfo,
                  val ideVersion: IdeVersion,
                  val verdict: Verdict,
                  val ignoredProblems: Set<Problem>) {
  override fun toString(): String = "Plugin $plugin and #$ideVersion: $verdict"
}