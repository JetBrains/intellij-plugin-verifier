package com.jetbrains.pluginverifier.tasks

import com.jetbrains.pluginverifier.output.settings.dependencies.MissingDependencyIgnoring
import com.jetbrains.pluginverifier.output.teamcity.TeamCityResultPrinter
import java.io.File

data class OutputOptions(val missingDependencyIgnoring: MissingDependencyIgnoring,
                         val needTeamCityLog: Boolean = false,
                         val teamCityGroupType: TeamCityResultPrinter.GroupBy = TeamCityResultPrinter.GroupBy.NOT_GROUPED,
                         val htmlReportFile: File? = null,
                         val dumpBrokenPluginsFile: String? = null)