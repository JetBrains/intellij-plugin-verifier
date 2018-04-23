package com.jetbrains.pluginverifier.output

import com.jetbrains.pluginverifier.output.settings.dependencies.MissingDependencyIgnoring
import com.jetbrains.pluginverifier.output.teamcity.TeamCityResultPrinter
import java.nio.file.Path

data class OutputOptions(val missingDependencyIgnoring: MissingDependencyIgnoring,
                         val needTeamCityLog: Boolean,
                         val teamCityGroupType: TeamCityResultPrinter.GroupBy,
                         val dumpBrokenPluginsFile: String?,
                         val verificationReportsDirectory: Path)
