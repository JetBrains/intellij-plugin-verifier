package com.jetbrains.pluginverifier.output

import com.jetbrains.pluginverifier.output.settings.dependencies.MissingDependencyIgnoring
import com.jetbrains.pluginverifier.output.teamcity.TeamCityResultPrinter
import java.io.File

data class OutputOptions(val missingDependencyIgnoring: MissingDependencyIgnoring,
                         val needTeamCityLog: Boolean,
                         val teamCityGroupType: TeamCityResultPrinter.GroupBy,
                         val htmlReportFile: File?,
                         val dumpBrokenPluginsFile: String?,
                         val verificationReportsDirectory: File)