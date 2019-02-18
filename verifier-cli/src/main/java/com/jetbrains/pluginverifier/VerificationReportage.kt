package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.presentation.DependenciesGraphPrettyPrinter
import com.jetbrains.pluginverifier.misc.buildList
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.misc.replaceInvalidFileNameCharacters
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.output.reporters.*
import com.jetbrains.pluginverifier.reporting.Reporter
import com.jetbrains.pluginverifier.reporting.common.CloseIgnoringDelegateReporter
import com.jetbrains.pluginverifier.reporting.common.FileReporter
import com.jetbrains.pluginverifier.reporting.common.LogReporter
import com.jetbrains.pluginverifier.reporting.common.MessageAndException
import com.jetbrains.pluginverifier.reporting.downloading.PluginDownloadReport
import com.jetbrains.pluginverifier.reporting.ignoring.ProblemIgnoredEvent
import com.jetbrains.pluginverifier.reporting.verification.Reportage
import com.jetbrains.pluginverifier.reporting.verification.Reporters
import com.jetbrains.pluginverifier.reporting.verification.ReportersProvider
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.results.experimental.ExperimentalApiUsage
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.results.structure.PluginStructureError
import com.jetbrains.pluginverifier.results.structure.PluginStructureWarning
import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths

/**
 * [ReportersProvider] that creates the following files layout
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
class VerificationReportage(private val outputOptions: OutputOptions) : Reportage {

  private val verificationLogger = LoggerFactory.getLogger("verification")
  private val messageReporters = listOf(LogReporter<String>(verificationLogger))
  private val ignoredPluginsReporters = listOf(IgnoredPluginsReporter(outputOptions))
  private val allIgnoredProblemsReporter = AllIgnoredProblemsReporter(outputOptions)
  private val allPluginDownloadingReporter = AllPluginDownloadingReporter(outputOptions, verificationLogger)

  override fun close() {
    messageReporters.forEach { it.closeLogged() }
    ignoredPluginsReporters.forEach { it.closeLogged() }
    allIgnoredProblemsReporter.closeLogged()
    allPluginDownloadingReporter.closeLogged()
  }

  override fun logVerificationStage(stageMessage: String) {
    messageReporters.forEach { it.report(stageMessage) }
  }

  override fun logPluginVerificationIgnored(
      pluginInfo: PluginInfo,
      verificationTarget: VerificationTarget,
      reason: String
  ) {
    ignoredPluginsReporters.forEach { it.report(PluginIgnoredEvent(pluginInfo, verificationTarget, reason)) }
  }

  @Synchronized
  override fun createPluginReporters(pluginInfo: PluginInfo, verificationTarget: VerificationTarget): Reporters {
    val pluginVerificationDirectory = outputOptions.getTargetReportDirectory(verificationTarget)
        .resolve("plugins")
        .resolve(createPluginVerificationDirectory(pluginInfo))

    return Reporters(
        createResultsReporters(pluginVerificationDirectory),
        createMessageReporters(pluginVerificationDirectory),
        emptyList(),
        createDownloadReporters(pluginVerificationDirectory),
        createPluginStructureWarningsReporters(pluginVerificationDirectory),
        createPluginStructureErrorsReporters(pluginVerificationDirectory),
        createProblemReporters(pluginVerificationDirectory),
        createDependencyGraphReporters(pluginVerificationDirectory),
        createIgnoredProblemReporters(pluginVerificationDirectory, verificationTarget),
        createDeprecatedReporters(pluginVerificationDirectory),
        createExceptionReporters(pluginVerificationDirectory),
        createExperimentalApiReporters(pluginVerificationDirectory)
    )
  }

  private fun createDownloadReporters(pluginVerificationDirectory: Path): List<Reporter<PluginDownloadReport>> =
      buildList {
        add(FileReporter(pluginVerificationDirectory.resolve("downloading.txt")))
        add(CloseIgnoringDelegateReporter(allPluginDownloadingReporter))
      }

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
      else -> Paths.get(pluginId, pluginInfo.version.replaceInvalidFileNameCharacters())
    }
  }

  private fun createPluginStructureWarningsReporters(pluginVerificationDirectory: Path) =
      buildList<Reporter<PluginStructureWarning>> {
        add(FileReporter(pluginVerificationDirectory.resolve("plugin-warnings.txt")))
      }

  private fun createPluginStructureErrorsReporters(pluginVerificationDirectory: Path) =
      buildList<Reporter<PluginStructureError>> {
        add(FileReporter(pluginVerificationDirectory.resolve("plugin-errors.txt")))
      }

  private fun createDeprecatedReporters(pluginVerificationDirectory: Path) =
      buildList<Reporter<DeprecatedApiUsage>> {
        add(FileReporter(pluginVerificationDirectory.resolve("deprecated-usages.txt")))
      }

  private fun createExperimentalApiReporters(pluginVerificationDirectory: Path) =
      buildList<Reporter<ExperimentalApiUsage>> {
        add(FileReporter(pluginVerificationDirectory.resolve("experimental-api-usages.txt")))
      }

  private fun createExceptionReporters(pluginVerificationDirectory: Path) =
      buildList<Reporter<MessageAndException>> {
        add(FileReporter(pluginVerificationDirectory.resolve("exception.txt")) {
          it.message + "\n" + ExceptionUtils.getStackTrace(it.exception)
        })
      }

  private fun createProblemReporters(pluginVerificationDirectory: Path) =
      buildList<Reporter<CompatibilityProblem>> {
        add(FileReporter(pluginVerificationDirectory.resolve("compatibility-problems.txt")))
      }

  private fun createDependencyGraphReporters(pluginVerificationDirectory: Path) =
      buildList<Reporter<DependenciesGraph>> {
        val file = pluginVerificationDirectory.resolve("dependencies.txt")
        val fileReporter = FileReporter<DependenciesGraph>(file, lineProvider = { graph ->
          DependenciesGraphPrettyPrinter(graph).prettyPresentation()
        })
        add(fileReporter)
      }

  private fun createResultsReporters(pluginVerificationDirectory: Path) =
      buildList<Reporter<VerificationResult>> {
        add(FileReporter(pluginVerificationDirectory.resolve("verification-verdict.txt")) { it.toString() })
      }

  private fun createIgnoredProblemReporters(
      pluginVerificationDirectory: Path,
      verificationTarget: VerificationTarget
  ) = buildList<Reporter<ProblemIgnoredEvent>> {
    add(CloseIgnoringDelegateReporter(allIgnoredProblemsReporter))
    add(IgnoredProblemsReporter(pluginVerificationDirectory, verificationTarget))
  }

  private fun createMessageReporters(pluginVerificationDirectory: Path) =
      buildList<Reporter<String>> {
        add(FileReporter(pluginVerificationDirectory.resolve("log.txt")))
      }

}