package com.jetbrains.pluginverifier

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.reporting.dependencies.DependencyGraphReporter
import com.jetbrains.pluginverifier.reporting.dependencies.FileDependencyGraphReporter
import com.jetbrains.pluginverifier.reporting.message.LogMessageReporter
import com.jetbrains.pluginverifier.reporting.problems.FileProblemReporter
import com.jetbrains.pluginverifier.reporting.problems.ProblemReporter
import com.jetbrains.pluginverifier.reporting.progress.LogProgressReporter
import com.jetbrains.pluginverifier.reporting.verdict.LogVerdictReporter
import com.jetbrains.pluginverifier.reporting.verification.ReporterSet
import com.jetbrains.pluginverifier.reporting.verification.ReporterSetProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

class MainVerificationReporterSetProvider(private val reportsDirectory: File?) : ReporterSetProvider {
  override fun provide(pluginCoordinate: PluginCoordinate, ideVersion: IdeVersion): ReporterSet {
    val pluginLoggerName = getPluginLoggerName(pluginCoordinate)
    val pluginLogger = LoggerFactory.getLogger(pluginLoggerName)
    val progressReporters = getProgressReporters(pluginCoordinate, ideVersion, pluginLogger)

    val problemsReporters = arrayListOf<ProblemReporter>()
    val dependenciesGraphReporters = arrayListOf<DependencyGraphReporter>()
    if (reportsDirectory != null) {
      val pluginVerificationDirectory = File(reportsDirectory, pluginLoggerName)
      val problemsFile = File(pluginVerificationDirectory, "problems.txt")
      val dependenciesFile = File(pluginVerificationDirectory, "dependencies.txt")

      val problemReporter = FileProblemReporter(problemsFile)
      dependenciesGraphReporters.add(createDependencyGraphReporter(dependenciesFile))
      problemsReporters.add(problemReporter)
    }

    return ReporterSet(
        verdictReporters = listOf(LogVerdictReporter(pluginLogger)),
        messageReporters = listOf(LogMessageReporter(pluginLogger)),
        progressReporters = progressReporters,
        warningReporters = emptyList(),
        problemsReporters = problemsReporters,
        dependenciesGraphReporters = dependenciesGraphReporters
    )
  }

  private fun getProgressReporters(pluginCoordinate: PluginCoordinate, ideVersion: IdeVersion, pluginLogger: Logger): List<LogProgressReporter> {
    val progressMessageProvider: (Double) -> String = getProgressMessageProvider(pluginCoordinate, ideVersion)
    val list = listOf(LogProgressReporter(pluginLogger, progressMessageProvider, step = 0.1))
    return emptyList()
  }

  private fun getProgressMessageProvider(pluginCoordinate: PluginCoordinate, ideVersion: IdeVersion): (Double) -> String = { progress ->
    buildString {
      append("Plugin $pluginCoordinate and #$ideVersion verification: ")
      if (progress == 1.0) {
        append("finished")
      } else {
        append("%.2f".format(progress * 100) + " % completed")
      }
    }
  }

  private fun createDependencyGraphReporter(dependenciesFile: File) =
      FileDependencyGraphReporter(dependenciesFile)

  private fun getPluginLoggerName(pluginCoordinate: PluginCoordinate) = pluginCoordinate.uniqueId
}