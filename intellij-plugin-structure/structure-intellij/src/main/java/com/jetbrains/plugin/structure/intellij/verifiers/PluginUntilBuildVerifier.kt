package com.jetbrains.plugin.structure.intellij.verifiers

import com.jetbrains.plugin.structure.intellij.beans.PluginBean
import com.jetbrains.plugin.structure.intellij.problems.ErroneousUntilBuild
import com.jetbrains.plugin.structure.intellij.problems.InvalidUntilBuild
import com.jetbrains.plugin.structure.intellij.problems.ProductCodePrefixInBuild
import com.jetbrains.plugin.structure.intellij.problems.SuspiciousUntilBuild
import com.jetbrains.plugin.structure.intellij.version.IdeVersion

class PluginUntilBuildVerifier {
  fun verify(plugin: PluginBean,
             descriptorPath: String,
             problemRegistrar: ProblemRegistrar) = with(problemRegistrar) {
    val untilBuild = plugin.ideaVersion?.untilBuild ?: return

    val untilBuildParsed = IdeVersion.createIdeVersionIfValid(untilBuild)
    if (untilBuildParsed == null) {
      registerProblem(InvalidUntilBuild(descriptorPath, untilBuild))
    } else {
      if (untilBuildParsed.baselineVersion > 999) {
        registerProblem(ErroneousUntilBuild(descriptorPath, untilBuildParsed))
      } else if (untilBuildParsed.baselineVersion > 400) {
        registerProblem(SuspiciousUntilBuild(untilBuild))
      }
      if (untilBuildParsed.productCode.isNotEmpty()) {
        registerProblem(ProductCodePrefixInBuild(descriptorPath))
      }
    }
  }
}