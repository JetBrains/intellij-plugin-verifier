package com.jetbrains.pluginverifier.reporting

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.base.utils.getStackTraceAsString
import com.jetbrains.plugin.structure.base.utils.replaceInvalidFileNameCharacters
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.presentation.DependenciesGraphPrettyPrinter
import com.jetbrains.pluginverifier.reporting.common.CloseIgnoringDelegateReporter
import com.jetbrains.pluginverifier.reporting.common.FileReporter
import com.jetbrains.pluginverifier.reporting.common.LogReporter
import com.jetbrains.pluginverifier.reporting.common.MessageAndException
import com.jetbrains.pluginverifier.reporting.ignoring.*
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.warnings.PluginStructureError
import com.jetbrains.pluginverifier.warnings.CompatibilityWarning
import com.jetbrains.pluginverifier.usages.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.usages.experimental.ExperimentalApiUsage
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Creates the following files layout for saving the verification reports:
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
class DirectoryBasePluginVerificationReportage(private val targetDirectoryProvider: (PluginVerificationTarget) -> Path) : PluginVerificationReportage {

  private val verificationLogger = LoggerFactory.getLogger("verification")
  private val messageReporters = listOf(LogReporter<String>(verificationLogger))
  private val ignoredPluginsReporters = listOf(IgnoredPluginsReporter(targetDirectoryProvider))
  private val allIgnoredProblemsReporter = AllIgnoredProblemsReporter(targetDirectoryProvider)

  override fun close() {
    messageReporters.forEach { it.closeLogged() }
    ignoredPluginsReporters.forEach { it.closeLogged() }
    allIgnoredProblemsReporter.closeLogged()
  }

  override fun logVerificationStage(stageMessage: String) {
    messageReporters.forEach { it.report(stageMessage) }
  }

  override fun logPluginVerificationIgnored(
      pluginInfo: PluginInfo,
      verificationTarget: PluginVerificationTarget,
      reason: String
  ) {
    ignoredPluginsReporters.forEach { it.report(PluginIgnoredEvent(pluginInfo, verificationTarget, reason)) }
  }

  @Synchronized
  override fun createPluginReporters(pluginInfo: PluginInfo, verificationTarget: PluginVerificationTarget): PluginReporters {
    val pluginVerificationDirectory = targetDirectoryProvider(verificationTarget)
        .resolve("plugins")
        .resolve(createPluginVerificationDirectory(pluginInfo))

    return PluginReporters(
        createResultsReporters(pluginVerificationDirectory),
        createMessageReporters(pluginVerificationDirectory),
        emptyList(),
        createWarningsReporters(pluginVerificationDirectory),
        createPluginStructureErrorsReporters(pluginVerificationDirectory),
        createProblemReporters(pluginVerificationDirectory),
        createDependencyGraphReporters(pluginVerificationDirectory),
        createIgnoredProblemReporters(pluginVerificationDirectory, verificationTarget),
        createDeprecatedReporters(pluginVerificationDirectory),
        createExceptionReporters(pluginVerificationDirectory),
        createExperimentalApiReporters(pluginVerificationDirectory)
    )
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
  private fun createPluginVerificationDirectory(pluginInfo: PluginInfo): Path {
    val pluginId = pluginInfo.pluginId.replaceInvalidFileNameCharacters()
    return when (pluginInfo) {
      is UpdateInfo -> {
        val version = "${pluginInfo.version} (#${pluginInfo.updateId})".replaceInvalidFileNameCharacters()
        Paths.get(pluginId, version)
      }
      else -> Paths.get(pluginId, pluginInfo.version.replaceInvalidFileNameCharacters())
    }
  }

  private fun createWarningsReporters(pluginVerificationDirectory: Path) =
      buildList<Reporter<CompatibilityWarning>> {
        add(FileReporter(pluginVerificationDirectory.resolve("warnings.txt")))
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
          it.message + "\n" + it.exception.getStackTraceAsString()
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
      buildList<Reporter<PluginVerificationResult>> {
        add(FileReporter(pluginVerificationDirectory.resolve("verification-verdict.txt")) { it.toString() })
      }

  private fun createIgnoredProblemReporters(
      pluginVerificationDirectory: Path,
      verificationTarget: PluginVerificationTarget
  ) = buildList<Reporter<ProblemIgnoredEvent>> {
    add(CloseIgnoringDelegateReporter(allIgnoredProblemsReporter))
    add(IgnoredProblemsReporter(pluginVerificationDirectory, verificationTarget))
  }

  private fun createMessageReporters(pluginVerificationDirectory: Path) =
      buildList<Reporter<String>> {
        add(FileReporter(pluginVerificationDirectory.resolve("log.txt")))
      }

  private inline fun <T> buildList(builderAction: MutableList<T>.() -> Unit): List<T> =
      arrayListOf<T>().apply(builderAction)
}