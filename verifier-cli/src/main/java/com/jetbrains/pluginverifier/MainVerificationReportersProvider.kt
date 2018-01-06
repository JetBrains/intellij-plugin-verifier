package com.jetbrains.pluginverifier

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.presentation.DependenciesGraphPrettyPrinter
import com.jetbrains.pluginverifier.misc.*
import com.jetbrains.pluginverifier.reporting.Reporter
import com.jetbrains.pluginverifier.reporting.common.CollectingReporter
import com.jetbrains.pluginverifier.reporting.common.FileReporter
import com.jetbrains.pluginverifier.reporting.common.LogReporter
import com.jetbrains.pluginverifier.reporting.ignoring.ProblemIgnoredEvent
import com.jetbrains.pluginverifier.reporting.progress.LogSteppedProgressReporter
import com.jetbrains.pluginverifier.reporting.verification.VerificationReporterSet
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportersProvider
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.repository.local.LocalPluginInfo
import com.jetbrains.pluginverifier.results.Verdict
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.results.problems.Problem
import com.jetbrains.pluginverifier.results.warnings.Warning
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths

class MainVerificationReportersProvider(override val globalMessageReporters: List<Reporter<String>>,
                                        override val globalProgressReporters: List<Reporter<Double>>,
                                        private val verificationReportsDirectory: Path,
                                        private val printPluginVerificationProgress: Boolean) : VerificationReportersProvider {

  companion object {
    private val LOG = LoggerFactory.getLogger(MainVerificationReportersProvider::class.java)
  }

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
  override fun getReporterSetForPluginVerification(pluginInfo: PluginInfo, ideVersion: IdeVersion): VerificationReporterSet {
    val ideResultsDirectory = getIdeResultsDirectory(ideVersion)
    val pluginVerificationDirectory = ideResultsDirectory.resolve("plugins").resolve(createPluginVerificationDirectory(pluginInfo))

    val pluginLogger = LoggerFactory.getLogger(pluginInfo.presentableName)

    return VerificationReporterSet(
        verdictReporters = createVerdictReporters(pluginLogger, pluginVerificationDirectory),
        messageReporters = createMessageReporters(pluginLogger),
        progressReporters = createProgressReporters(pluginInfo, ideVersion, pluginLogger),
        warningReporters = createWarningReporters(pluginVerificationDirectory),
        problemsReporters = createProblemReporters(pluginVerificationDirectory),
        dependenciesGraphReporters = createDependencyGraphReporters(pluginVerificationDirectory),
        ignoredProblemReporters = createIgnoredProblemReporters(pluginLogger, pluginVerificationDirectory, ideVersion),
        deprecatedReporters = createDeprecatedReporters(pluginVerificationDirectory)
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
  private fun createPluginVerificationDirectory(pluginInfo: PluginInfo): Path =
      when (pluginInfo) {
        is UpdateInfo -> {
          val pluginId = pluginInfo.pluginId.replaceInvalidFileNameCharacters()
          val version = "${pluginInfo.version} (#${pluginInfo.updateId})".replaceInvalidFileNameCharacters()
          Paths.get(pluginId, version)
        }
        is LocalPluginInfo -> Paths.get(pluginInfo.pluginFile.simpleName)
        else -> Paths.get(pluginInfo.presentableName)
      }

  private fun createWarningReporters(pluginVerificationDirectory: Path) = buildList<Reporter<Warning>> {
    add(FileReporter(pluginVerificationDirectory.resolve("warnings.txt")))
  }

  private fun createDeprecatedReporters(pluginVerificationDirectory: Path) = buildList<Reporter<DeprecatedApiUsage>> {
    add(FileReporter(pluginVerificationDirectory.resolve("deprecated-usages.txt")))
  }

  private fun createMessageReporters(pluginLogger: Logger) = buildList<Reporter<String>> {
    add(LogReporter(pluginLogger))
  }

  private fun createProblemReporters(pluginVerificationDirectory: Path) = buildList<Reporter<Problem>> {
    add(FileReporter(pluginVerificationDirectory.resolve("problems.txt")))
  }

  private fun createDependencyGraphReporters(pluginVerificationDirectory: Path) = buildList<Reporter<DependenciesGraph>> {
    val file = pluginVerificationDirectory.resolve("dependencies.txt")
    val fileReporter = FileReporter<DependenciesGraph>(file, lineProvider = { graph ->
      DependenciesGraphPrettyPrinter(graph).prettyPresentation()
    })
    add(fileReporter)
  }

  private fun createVerdictReporters(pluginLogger: Logger, pluginVerificationDirectory: Path) = buildList<Reporter<Verdict>> {
    if (pluginLogger.isDebugEnabled) {
      add(LogReporter(pluginLogger))
    }
    add(FileReporter(pluginVerificationDirectory.resolve("verdict.txt")))
  }

  private fun createIgnoredProblemReporters(pluginLogger: Logger,
                                            pluginVerificationDirectory: Path,
                                            ideVersion: IdeVersion) = buildList<Reporter<ProblemIgnoredEvent>> {
    val ideCollectingProblemsReporter = ideVersion2AllIgnoredProblemsReporter.getOrPut(ideVersion) { CollectingReporter() }
    add(ideCollectingProblemsReporter)
    if (pluginLogger.isDebugEnabled) {
      add(LogReporter(pluginLogger))
    }
    add(FileReporter(pluginVerificationDirectory.resolve("ignored-problems.txt")))
  }

  private fun createProgressReporters(pluginInfo: PluginInfo, ideVersion: IdeVersion, pluginLogger: Logger) = buildList<LogSteppedProgressReporter> {
    if (printPluginVerificationProgress) {
      val logMessageProvider = createProgressMessageProvider(pluginInfo, ideVersion)
      add(LogSteppedProgressReporter(pluginLogger, logMessageProvider, step = 0.1))
    }
  }

  private fun createProgressMessageProvider(pluginInfo: PluginInfo, ideVersion: IdeVersion): (Double) -> String = { progress ->
    buildString {
      append("Plugin $pluginInfo and #$ideVersion verification: ")
      if (progress == 1.0) {
        append("100% finished")
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
        try {
          ignoredProblemsFile.writeText(ignoredProblemsText)
        } catch (e: Exception) {
          LOG.error("Unable to save ignored problems of $ideVersion", e)
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
            appendln("    $shortDescription:")
            for ((plugin, allWithPlugin) in allWithShortDescription.groupBy { it.plugin }) {
              appendln("      $plugin:")
              for (ignoredEvent in allWithPlugin) {
                appendln("        ${ignoredEvent.problem.fullDescription}")
              }
            }
            appendln()
          }
        }
      }

}