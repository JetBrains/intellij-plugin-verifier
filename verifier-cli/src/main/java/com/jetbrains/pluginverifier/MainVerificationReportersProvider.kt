package com.jetbrains.pluginverifier

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.presentation.DependenciesGraphPrettyPrinter
import com.jetbrains.pluginverifier.misc.buildList
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.misc.doLogged
import com.jetbrains.pluginverifier.misc.replaceInvalidFileNameCharacters
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.reporting.Reporter
import com.jetbrains.pluginverifier.reporting.common.CollectingReporter
import com.jetbrains.pluginverifier.reporting.common.FileReporter
import com.jetbrains.pluginverifier.reporting.common.LogReporter
import com.jetbrains.pluginverifier.reporting.ignoring.ProblemIgnoredEvent
import com.jetbrains.pluginverifier.reporting.progress.LogSteppedProgressReporter
import com.jetbrains.pluginverifier.reporting.verification.VerificationReporterSet
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportersProvider
import com.jetbrains.pluginverifier.results.Verdict
import com.jetbrains.pluginverifier.results.problems.Problem
import com.jetbrains.pluginverifier.results.warnings.Warning
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

class MainVerificationReportersProvider(override val globalMessageReporters: List<Reporter<String>>,
                                        override val globalProgressReporters: List<Reporter<Double>>,
                                        private val verificationReportsDirectory: File,
                                        private val printPluginVerificationProgress: Boolean) : VerificationReportersProvider {

  private val ideVersion2AllIgnoredProblemsReporter = hashMapOf<IdeVersion, CollectingReporter<ProblemIgnoredEvent>>()

  /**
   * The layout of directories looks like this:
   * verification-TIMESTAMP/
   *     IU-171.1234/
   *         all-ignored-problems.txt
   *         plugins/
   *             com.plugin.one/
   *                 1.0/
   *                     warnings.txt
   *                     problems.txt
   *                     verdict.txt
   *                     dependencies.txt
   *                     ignored-problems.txt
   *                 2.0/
   *                     ...
   *             com.another.plugin/
   *                 1.5.0/
   *                     ...
   *     IU-172.5678/
   *         all-ignored-problems.txt
   *         plugins/
   *             com.third.plugin/
   *                 ...
   */
  override fun getReporterSetForPluginVerification(pluginCoordinate: PluginCoordinate, ideVersion: IdeVersion): VerificationReporterSet {
    val ideResultsDirectory = getIdeResultsDirectory(ideVersion)
    val pluginVerificationDirectory = ideResultsDirectory.resolve("plugins").resolve(createPluginVerificationDirectory(pluginCoordinate))

    val pluginLogger = LoggerFactory.getLogger(pluginCoordinate.presentableName)

    return VerificationReporterSet(
        verdictReporters = createVerdictReporters(pluginLogger, pluginVerificationDirectory),
        messageReporters = createMessageReporters(pluginLogger),
        progressReporters = createProgressReporters(pluginCoordinate, ideVersion, pluginLogger),
        warningReporters = createWarningReporters(pluginVerificationDirectory),
        problemsReporters = createProblemReporters(pluginVerificationDirectory),
        dependenciesGraphReporters = createDependencyGraphReporters(pluginVerificationDirectory),
        ignoredProblemReporters = createIgnoredProblemReporters(pluginLogger, pluginVerificationDirectory, ideVersion)
    )
  }

  private fun getIdeResultsDirectory(ideVersion: IdeVersion) =
      verificationReportsDirectory.resolve("$ideVersion".replaceInvalidFileNameCharacters())

  /**
   * Creates a directory for reports of the plugin in the verified IDE:
   * com.plugin.id/  <- if the plugin is specified by its plugin-id and version
   *     1.0.0/
   *          ....
   *     2.0.0/
   * plugin.zip/     <- if the plugin is specified by the local file path
   *     ....
   */
  private fun createPluginVerificationDirectory(pluginCoordinate: PluginCoordinate): File =
      when (pluginCoordinate) {
        is PluginCoordinate.ByUpdateInfo -> {
          val updateInfo = pluginCoordinate.updateInfo
          val pluginId = updateInfo.pluginId.replaceInvalidFileNameCharacters()
          val version = "${updateInfo.version} (#${updateInfo.updateId})".replaceInvalidFileNameCharacters()
          File(pluginId, version)
        }
        is PluginCoordinate.ByFile -> {
          File(pluginCoordinate.pluginFile.name)
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
                                            ideVersion: IdeVersion) = buildList<Reporter<ProblemIgnoredEvent>> {
    val ideCollectingProblemsReporter = ideVersion2AllIgnoredProblemsReporter.getOrPut(ideVersion) { CollectingReporter() }
    add(ideCollectingProblemsReporter)
    if (pluginLogger.isDebugEnabled) {
      add(LogReporter(pluginLogger))
    }
    add(FileReporter(File(pluginVerificationDirectory, "ignored-problems.txt")))
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

  override fun close() {
    globalMessageReporters.forEach { it.closeLogged() }
    globalProgressReporters.forEach { it.closeLogged() }
    saveIdeIgnoredProblems()
  }

  private fun saveIdeIgnoredProblems() {
    for ((ideVersion, collectingReporter) in ideVersion2AllIgnoredProblemsReporter) {
      val allIdeIgnoredProblems = collectingReporter.getReported()
      if (allIdeIgnoredProblems.isNotEmpty()) {
        val ignoredProblemsText = formatIgnoredProblemsOfIde(ideVersion, allIdeIgnoredProblems)
        val ignoredProblemsFile = getIdeResultsDirectory(ideVersion).resolve("all-ignored-problems.txt")
        ignoredProblemsFile.doLogged("save ignored problems of $ideVersion") {
          writeText(ignoredProblemsText)
        }
      }
    }
    ideVersion2AllIgnoredProblemsReporter.values.forEach { it.closeLogged() }
  }

  private fun formatIgnoredProblemsOfIde(ideVersion: IdeVersion, allIdeIgnoredProblems: List<ProblemIgnoredEvent>): String =
      buildString {
        appendln("The following problems of $ideVersion were ignored:")
        for ((reason, allWithReason) in allIdeIgnoredProblems.groupBy { it.reason }) {
          appendln("because $reason:")
          for ((shortDescription, allWithShortDescription) in allWithReason.groupBy { it.problem.shortDescription }) {
            appendln("  $shortDescription:")
            for ((plugin, allWithPlugin) in allWithShortDescription.groupBy { it.plugin }) {
              appendln("    $plugin:")
              for (ignoredEvent in allWithPlugin) {
                appendln("      ${ignoredEvent.problem.fullDescription}")
              }
            }
          }
        }
      }

}