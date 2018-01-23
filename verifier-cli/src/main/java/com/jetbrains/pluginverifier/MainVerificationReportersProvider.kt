package com.jetbrains.pluginverifier

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.presentation.DependenciesGraphPrettyPrinter
import com.jetbrains.pluginverifier.misc.*
import com.jetbrains.pluginverifier.reporting.Reporter
import com.jetbrains.pluginverifier.reporting.common.CollectingReporter
import com.jetbrains.pluginverifier.reporting.common.FileReporter
import com.jetbrains.pluginverifier.reporting.common.LogReporter
import com.jetbrains.pluginverifier.reporting.common.MessageAndException
import com.jetbrains.pluginverifier.reporting.ignoring.ProblemIgnoredEvent
import com.jetbrains.pluginverifier.reporting.progress.LogSteppedProgressReporter
import com.jetbrains.pluginverifier.reporting.verification.VerificationReporterSet
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportersProvider
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.repository.local.LocalPluginInfo
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.results.structure.PluginStructureError
import com.jetbrains.pluginverifier.results.structure.PluginStructureWarning
import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths

/**
 * [VerificationReportersProvider] that creates the following files layout
 * for saving the verification reports:
 * ```
 * <verification-dir>/
 *     <IDE #1>/
 *         all-ignored-problems.txt
 *         plugins/
 *             com.plugin.one/
 *                 1.0/
 *                     warnings.txt
 *                     problems.txt
 *                     verification-result.txt
 *                     dependencies.txt
 *                     ignored-problems.txt
 *                 2.0/
 *                     ...
 *             com.another.plugin/
 *                 1.5.0/
 *                     ...
 *     <IDE #2>/
 *         all-ignored-problems.txt
 *         plugins/
 *             com.third.plugin/
 *                 ...
 * ```
 */
class MainVerificationReportersProvider(override val globalMessageReporters: List<Reporter<String>>,
                                        override val globalProgressReporters: List<Reporter<Double>>,
                                        private val verificationReportsDirectory: Path,
                                        private val printPluginVerificationProgress: Boolean,
                                        private val logger: Logger) : VerificationReportersProvider {

  companion object {
    private val LOG = LoggerFactory.getLogger(MainVerificationReportersProvider::class.java)
  }

  private val ideVersion2AllIgnoredProblemsReporter = hashMapOf<IdeVersion, CollectingReporter<ProblemIgnoredEvent>>()

  override fun getReporterSetForPluginVerification(pluginInfo: PluginInfo, ideVersion: IdeVersion): VerificationReporterSet {
    val ideResultsDirectory = getIdeResultsDirectory(ideVersion)
    val pluginVerificationDirectory = ideResultsDirectory.resolve("plugins").resolve(createPluginVerificationDirectory(pluginInfo))

    return VerificationReporterSet(
        verificationResultReporters = createResultsReporters(pluginVerificationDirectory),
        messageReporters = createMessageReporters(),
        progressReporters = createProgressReporters(pluginInfo, ideVersion),
        pluginStructureWarningsReporters = createPluginStructureWarningsReporters(pluginVerificationDirectory),
        pluginStructureErrorsReporters = createPluginStructureErrorsReporters(pluginVerificationDirectory),
        problemsReporters = createProblemReporters(pluginVerificationDirectory),
        dependenciesGraphReporters = createDependencyGraphReporters(pluginVerificationDirectory),
        ignoredProblemReporters = createIgnoredProblemReporters(pluginVerificationDirectory, ideVersion),
        deprecatedReporters = createDeprecatedReporters(pluginVerificationDirectory),
        exceptionReporters = createExceptionReporters(pluginVerificationDirectory)
    )
  }


  private fun getIdeResultsDirectory(ideVersion: IdeVersion) =
      verificationReportsDirectory.resolve("$ideVersion".replaceInvalidFileNameCharacters())

  /**
   * Creates a directory for reports of the plugin in the verified IDE:
   * ```
   * com.plugin.id/  <- if the plugin is specified by its plugin-id and version
   *     1.0.0/
   *          ....
   *     2.0.0/
   * plugin.zip/     <- if the plugin is specified by the local file path
   *     ....
   * ```
   */
  private fun createPluginVerificationDirectory(pluginInfo: PluginInfo): Path? {
    val pluginId = pluginInfo.pluginId.replaceInvalidFileNameCharacters()
    return when (pluginInfo) {
      is UpdateInfo -> {
        val version = "${pluginInfo.version} (#${pluginInfo.updateId})".replaceInvalidFileNameCharacters()
        Paths.get(pluginId, version)
      }
      is LocalPluginInfo -> {
        val version = "${pluginInfo.version} (${pluginInfo.pluginFile.simpleName})".replaceInvalidFileNameCharacters()
        Paths.get(pluginId, version)
      }
      else -> Paths.get(pluginId, pluginInfo.version.replaceInvalidFileNameCharacters())
    }
  }

  private fun createPluginStructureWarningsReporters(pluginVerificationDirectory: Path) = buildList<Reporter<PluginStructureWarning>> {
    add(FileReporter(pluginVerificationDirectory.resolve("plugin-warnings.txt")))
  }

  private fun createPluginStructureErrorsReporters(pluginVerificationDirectory: Path) = buildList<Reporter<PluginStructureError>> {
    add(FileReporter(pluginVerificationDirectory.resolve("plugin-errors.txt")))
  }

  private fun createDeprecatedReporters(pluginVerificationDirectory: Path) = buildList<Reporter<DeprecatedApiUsage>> {
    add(FileReporter(pluginVerificationDirectory.resolve("deprecated-usages.txt")))
  }

  private fun createExceptionReporters(pluginVerificationDirectory: Path) = buildList<Reporter<MessageAndException>> {
    add(FileReporter(pluginVerificationDirectory.resolve("exception.txt")) {
      it.message + "\n" + ExceptionUtils.getStackTrace(it.exception)
    })
    add(object : LogReporter<MessageAndException>(logger) {
      override fun report(t: MessageAndException) {
        logger.info(t.message, t.exception)
      }
    })
  }

  private fun createMessageReporters() = buildList<Reporter<String>> {
    add(LogReporter(logger))
  }

  private fun createProblemReporters(pluginVerificationDirectory: Path) = buildList<Reporter<CompatibilityProblem>> {
    add(FileReporter(pluginVerificationDirectory.resolve("problems.txt")))
  }

  private fun createDependencyGraphReporters(pluginVerificationDirectory: Path) = buildList<Reporter<DependenciesGraph>> {
    val file = pluginVerificationDirectory.resolve("dependencies.txt")
    val fileReporter = FileReporter<DependenciesGraph>(file, lineProvider = { graph ->
      DependenciesGraphPrettyPrinter(graph).prettyPresentation()
    })
    add(fileReporter)
  }

  private fun createResultsReporters(pluginVerificationDirectory: Path) = buildList<Reporter<VerificationResult>> {
    if (logger.isDebugEnabled) {
      add(LogReporter(logger))
    }
    add(FileReporter(pluginVerificationDirectory.resolve("verification-result.txt")))
  }

  private fun createIgnoredProblemReporters(pluginVerificationDirectory: Path,
                                            ideVersion: IdeVersion) = buildList<Reporter<ProblemIgnoredEvent>> {
    val ideCollectingProblemsReporter = ideVersion2AllIgnoredProblemsReporter.getOrPut(ideVersion) { CollectingReporter() }
    add(ideCollectingProblemsReporter)
    if (logger.isDebugEnabled) {
      add(LogReporter(logger))
    }
    add(FileReporter(pluginVerificationDirectory.resolve("ignored-problems.txt")))
  }

  private fun createProgressReporters(pluginInfo: PluginInfo, ideVersion: IdeVersion) = buildList<LogSteppedProgressReporter> {
    if (printPluginVerificationProgress) {
      val logMessageProvider = createProgressMessageProvider(pluginInfo, ideVersion)
      add(LogSteppedProgressReporter(logger, logMessageProvider, step = 0.1))
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