package com.jetbrains.plugin.structure.intellij.verifiers

import com.jetbrains.plugin.structure.intellij.beans.PluginBean
import com.jetbrains.plugin.structure.intellij.problems.InvalidUntilBuild
import com.jetbrains.plugin.structure.intellij.problems.ProductCodePrefixInBuild
import com.jetbrains.plugin.structure.intellij.problems.SuspiciousUntilBuild
import com.jetbrains.plugin.structure.intellij.version.IdeVersion


private const val BUILD_NUMBER = "__BUILD_NUMBER__"
private const val SNAPSHOT = "SNAPSHOT"

class PluginUntilBuildVerifier {
  fun verify(plugin: PluginBean,
             descriptorPath: String,
             problemRegistrar: ProblemRegistrar) = with(problemRegistrar) {
    val untilBuild = plugin.ideaVersion?.untilBuild ?: return
    if (isJustASingleComponent(untilBuild)) {
      if (isSpecialSingleComponent(untilBuild)) {
        return
      }
      verifySingleComponentUntilBuild(untilBuild, descriptorPath, problemRegistrar)
      return
    }

    val untilBuildParsed = IdeVersion.createIdeVersionIfValid(untilBuild)
    if (untilBuildParsed == null) {
      registerProblem(InvalidUntilBuild(descriptorPath, untilBuild))
    } else {
      if (untilBuildParsed.baselineVersion >= 999) {
        registerProblem(InvalidUntilBuild(descriptorPath, untilBuild, untilBuildParsed))
      } else if (untilBuildParsed.baselineVersion > 400) {
        registerProblem(SuspiciousUntilBuild(untilBuild))
      }
      if (untilBuildParsed.productCode.isNotEmpty()) {
        registerProblem(ProductCodePrefixInBuild(descriptorPath))
      }
    }
  }

  private fun isSpecialSingleComponent(untilBuild: String): Boolean {
    return BUILD_NUMBER == untilBuild || SNAPSHOT == untilBuild
  }

  private fun verifySingleComponentUntilBuild(untilBuild: String,
                                              descriptorPath: String,
                                              problemRegistrar: ProblemRegistrar) {
    try {
      val untilBuildNumber = untilBuild.toInt()
      if (untilBuildNumber >= 400) {
        problemRegistrar.registerProblem(InvalidUntilBuild(descriptorPath, untilBuild))
      } else if (untilBuildNumber >= 300) {
        problemRegistrar.registerProblem(SuspiciousUntilBuild(untilBuild))
      }
    } catch (e: NumberFormatException) {
      problemRegistrar.registerProblem(InvalidUntilBuild(descriptorPath, untilBuild))
    }
  }

  private fun isJustASingleComponent(untilBuild: String): Boolean {
    if (!untilBuild.contains('.') && untilBuild.isNotBlank()) {
      return true
    }
    val components = untilBuild.split('.').filterNot {
      it.isNotBlank()
    }.size
    return components == 1
  }
}