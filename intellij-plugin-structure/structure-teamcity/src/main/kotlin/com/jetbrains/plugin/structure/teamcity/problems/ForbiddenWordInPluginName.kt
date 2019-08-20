package com.jetbrains.plugin.structure.teamcity.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

object ForbiddenWordInPluginName : PluginProblem() {
  val forbiddenWords = listOf("teamcity", "plugin")

  override val level
    get() = Level.ERROR

  override val message
    get() = "Plugin name should not contain the following words: ${forbiddenWords.joinToString()}"

}