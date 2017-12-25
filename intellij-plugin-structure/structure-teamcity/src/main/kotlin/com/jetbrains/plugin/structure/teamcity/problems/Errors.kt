package com.jetbrains.plugin.structure.teamcity.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

object ForbiddenWordInPluginName :
    PluginProblem() {
  val forbiddenWords = listOf("teamcity", "plugin")

  override val level: PluginProblem.Level = PluginProblem.Level.ERROR

  override val message: String =
      "Plugin name should not contain the following words: ${forbiddenWords.joinToString(", ")}"


}