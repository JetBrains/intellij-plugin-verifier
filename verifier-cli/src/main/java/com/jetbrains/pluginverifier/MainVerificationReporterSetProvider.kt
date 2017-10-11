package com.jetbrains.pluginverifier

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.presentation.DependenciesGraphPrettyPrinter
import com.jetbrains.pluginverifier.misc.buildList
import com.jetbrains.pluginverifier.misc.replaceInvalidFileNameCharacters
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.reporting.Reporter
import com.jetbrains.pluginverifier.reporting.common.FileReporter
import com.jetbrains.pluginverifier.reporting.common.LogReporter
import com.jetbrains.pluginverifier.reporting.ignoring.ProblemIgnoredEvent
import com.jetbrains.pluginverifier.reporting.progress.LogSteppedProgressReporter
import com.jetbrains.pluginverifier.reporting.verification.ReporterSet
import com.jetbrains.pluginverifier.reporting.verification.ReporterSetProvider
import com.jetbrains.pluginverifier.results.Verdict
import com.jetbrains.pluginverifier.results.problems.Problem
import com.jetbrains.pluginverifier.results.warnings.Warning
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

class MainVerificationReporterSetProvider(private val verificationReportsDirectory: File,
                                          private val commonReporterForIgnoredProblems: Reporter<String>,
                                          private val printPluginVerificationProgress: Boolean) : ReporterSetProvider {

  override fun provide(pluginCoordinate: PluginCoordinate, ideVersion: IdeVersion): ReporterSet {
    val pluginsResultsDirectory = File(verificationReportsDirectory, "$ideVersion".replaceInvalidFileNameCharacters())
    val pluginVerificationDirectory = createPluginVerificationDirectory(pluginsResultsDirectory, pluginCoordinate)

    val pluginLogger = LoggerFactory.getLogger(pluginCoordinate.presentableName)

    return ReporterSet(
        verdictReporters = createVerdictReporters(pluginLogger, pluginVerificationDirectory),
        messageReporters = createMessageReporters(pluginLogger),
        progressReporters = createProgressReporters(pluginCoordinate, ideVersion, pluginLogger),
        warningReporters = createWarningReporters(pluginVerificationDirectory),
        problemsReporters = createProblemReporters(pluginVerificationDirectory),
        dependenciesGraphReporters = createDependencyGraphReporters(pluginVerificationDirectory),
        ignoredProblemReporters = createIgnoredProblemReporters(pluginLogger, pluginVerificationDirectory, pluginCoordinate, ideVersion)
    )
  }

  private fun createPluginVerificationDirectory(parentDir: File, pluginCoordinate: PluginCoordinate): File =
      when (pluginCoordinate) {
        is PluginCoordinate.ByUpdateInfo -> {
          val updateInfo = pluginCoordinate.updateInfo
          val pluginId = updateInfo.pluginId.replaceInvalidFileNameCharacters()
          val folderById = File(parentDir, pluginId)
          val version = "${updateInfo.version} (#${updateInfo.updateId})".replaceInvalidFileNameCharacters()
          File(folderById, version)
        }
        is PluginCoordinate.ByFile -> {
          File(parentDir, pluginCoordinate.pluginFile.name)
        }
      }

  private fun createWarningReporters(pluginVerificationDirectory: File) = buildList<Reporter<Warning>> {
    add(FileReporter(File(pluginVerificationDirectory, "warnings.txt")))
  }

  private fun createMessageReporters(pluginLogger: Logger) = buildList<Reporter<String>> {
    add(LogReporter(pluginLogger))
  }

  private fun createProblemReporters(pluginVerificationDirectory: File) = buildList<Reporter<Problem>> {
    add(FileReporter(File(pluginVerificationDirectory, "problems.txt")))
  }

  private fun createDependencyGraphReporters(pluginVerificationDirectory: File) = buildList<Reporter<DependenciesGraph>> {
    val file = File(pluginVerificationDirectory, "dependencies.txt")
    val fileReporter = FileReporter<DependenciesGraph>(file, lineProvider = { graph ->
      DependenciesGraphPrettyPrinter(graph).prettyPresentation()
    })
    add(fileReporter)
  }

  private fun createVerdictReporters(pluginLogger: Logger, pluginVerificationDirectory: File) = buildList<Reporter<Verdict>> {
    if (pluginLogger.isDebugEnabled) {
      add(LogReporter(pluginLogger))
    }
    add(FileReporter(File(pluginVerificationDirectory, "verdict.txt")))
  }

  private fun createIgnoredProblemReporters(pluginLogger: Logger,
                                            pluginVerificationDirectory: File,
                                            pluginCoordinate: PluginCoordinate,
                                            ideVersion: IdeVersion) = buildList<Reporter<ProblemIgnoredEvent>> {
    val ignoredProblemsReporter = createPluginIgnoredProblemReporter(pluginCoordinate, ideVersion)
    add(ignoredProblemsReporter)
    if (pluginLogger.isDebugEnabled) {
      add(LogReporter(pluginLogger))
    }
    add(FileReporter(File(pluginVerificationDirectory, "ignored-problems.txt")))
  }

  private fun createPluginIgnoredProblemReporter(pluginCoordinate: PluginCoordinate, ideVersion: IdeVersion) = object : Reporter<ProblemIgnoredEvent> {

    private val pluginIgnoredProblems = hashSetOf<ProblemIgnoredEvent>()

    override fun report(t: ProblemIgnoredEvent) {
      pluginIgnoredProblems.add(t)
    }

    override fun close() {
      pluginIgnoredProblems.groupBy({ it.reason }, { it.problem }).forEach { (reason, problems) ->
        val message = "Following problems of the $pluginCoordinate in $ideVersion were ignored: $reason:\n" + problems.joinToString("\n") { "    $it" } + "\n"
        commonReporterForIgnoredProblems.report(message)
      }
    }
  }

  private fun createProgressReporters(pluginCoordinate: PluginCoordinate, ideVersion: IdeVersion, pluginLogger: Logger) = buildList<LogSteppedProgressReporter> {
    if (printPluginVerificationProgress) {
      val logMessageProvider = createProgressMessageProvider(pluginCoordinate, ideVersion)
      add(LogSteppedProgressReporter(pluginLogger, logMessageProvider, step = 0.1))
    }
  }

  private fun createProgressMessageProvider(pluginCoordinate: PluginCoordinate, ideVersion: IdeVersion): (Double) -> String = { progress ->
    buildString {
      append("Plugin $pluginCoordinate and #$ideVersion verification: ")
      if (progress == 1.0) {
        append("finished")
      } else {
        append("%.2f".format(progress * 100) + " % completed")
      }
    }
  }

}