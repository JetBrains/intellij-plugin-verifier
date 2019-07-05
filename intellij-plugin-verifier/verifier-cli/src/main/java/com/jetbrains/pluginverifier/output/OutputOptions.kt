package com.jetbrains.pluginverifier.output

import com.jetbrains.plugin.structure.base.utils.replaceInvalidFileNameCharacters
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.output.teamcity.TeamCityLog
import com.jetbrains.pluginverifier.output.teamcity.TeamCityResultPrinter
import java.nio.file.Path

data class OutputOptions(
    private val verificationReportsDirectory: Path,
    val teamCityLog: TeamCityLog?,
    val teamCityGroupType: TeamCityResultPrinter.GroupBy,
    val dumpBrokenPluginsFile: String?
) {

  fun getTargetReportDirectory(verificationTarget: VerificationTarget): Path = when (verificationTarget) {
    is VerificationTarget.Ide -> verificationReportsDirectory
        .resolve(verificationTarget.ideVersion.asString().replaceInvalidFileNameCharacters())

    is VerificationTarget.Plugin -> verificationReportsDirectory
        .resolve("${verificationTarget.plugin.pluginId} ${verificationTarget.plugin.version}".replaceInvalidFileNameCharacters())
  }

}
