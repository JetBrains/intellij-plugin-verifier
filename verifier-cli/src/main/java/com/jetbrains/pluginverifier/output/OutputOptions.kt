package com.jetbrains.pluginverifier.output

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.replaceInvalidFileNameCharacters
import com.jetbrains.pluginverifier.output.html.HtmlResultPrinter
import com.jetbrains.pluginverifier.output.settings.dependencies.MissingDependencyIgnoring
import com.jetbrains.pluginverifier.output.teamcity.TeamCityResultPrinter
import com.jetbrains.pluginverifier.parameters.filtering.PluginIdAndVersion
import com.jetbrains.pluginverifier.results.VerificationResult
import java.nio.file.Path

data class OutputOptions(val missingDependencyIgnoring: MissingDependencyIgnoring,
                         val needTeamCityLog: Boolean,
                         val teamCityGroupType: TeamCityResultPrinter.GroupBy,
                         val dumpBrokenPluginsFile: String?,
                         val verificationReportsDirectory: Path) {

  fun saveToHtmlFile(ideVersion: IdeVersion,
                     excludedPlugins: List<PluginIdAndVersion>,
                     results: List<VerificationResult>) {

    val htmlReportFile = verificationReportsDirectory
        .resolve(ideVersion.toString().replaceInvalidFileNameCharacters())
        .resolve("report.html")

    val isExcluded: (PluginIdAndVersion) -> Boolean = { it in excludedPlugins }
    val htmlResultPrinter = HtmlResultPrinter(ideVersion, isExcluded, htmlReportFile, missingDependencyIgnoring)
    htmlResultPrinter.printResults(results)
  }

}
