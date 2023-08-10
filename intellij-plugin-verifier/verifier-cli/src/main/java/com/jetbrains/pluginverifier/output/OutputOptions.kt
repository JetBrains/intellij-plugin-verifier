/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.output

import com.jetbrains.plugin.structure.base.utils.replaceInvalidFileNameCharacters
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.output.teamcity.TeamCityHistory
import com.jetbrains.pluginverifier.output.teamcity.TeamCityLog
import com.jetbrains.pluginverifier.output.teamcity.TeamCityResultPrinter
import java.nio.file.Path

data class OutputOptions(
  private val verificationReportsDirectory: Path,
  val teamCityLog: TeamCityLog?,
  val teamCityGroupType: TeamCityResultPrinter.GroupBy,
  val previousTcHistory: TeamCityHistory?,
  val outputFormats: List<OutputFormat> = DEFAULT_OUTPUT_FORMATS
) {

  fun getTargetReportDirectory(verificationTarget: PluginVerificationTarget): Path = when (verificationTarget) {
    is PluginVerificationTarget.IDE -> verificationReportsDirectory
      .resolve(verificationTarget.ideVersion.asString().replaceInvalidFileNameCharacters())

    is PluginVerificationTarget.Plugin -> verificationReportsDirectory
      .resolve("${verificationTarget.plugin.pluginId} ${verificationTarget.plugin.version}".replaceInvalidFileNameCharacters())
  }

  fun postProcessTeamCityTests(newTcHistory: TeamCityHistory) {
    if (teamCityLog != null) {
      newTcHistory.writeToFile(verificationReportsDirectory.resolve("tc-tests.json"))
      if (previousTcHistory != null) {
        newTcHistory.reportOldSkippedTestsSuccessful(previousTcHistory, teamCityLog)
      }
    }
  }

}
