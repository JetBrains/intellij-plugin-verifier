/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.reporting

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.base.utils.replaceInvalidFileNameCharacters
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.dependencies.presentation.DependenciesGraphPrettyPrinter
import com.jetbrains.pluginverifier.reporting.common.FileReporter
import com.jetbrains.pluginverifier.reporting.common.LogReporter
import com.jetbrains.pluginverifier.reporting.ignoring.*
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo
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
 *                     verification-verdict.txt
 *                     compatibility-warnings.txt
 *                     compatibility-problems.txt
 *                     dependencies.txt
 *                     deprecated-usages.txt
 *                     experimental-api-usages.txt
 *                     internal-api-usages.txt
 *                     override-only-usages.txt
 *                     non-extendable-api-usages.txt
 *                     plugin-structure-warnings.txt
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
class DirectoryBasedPluginVerificationReportage(
  private val pluginVerificationReportageResultAggregator: PluginVerificationReportageAggregator = PluginVerificationReportageAggregator { _, _ -> },
  private val targetDirectoryProvider: (PluginVerificationTarget) -> Path,
  ) : PluginVerificationReportage {

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

  private fun <T> Reporter<T>.useReporter(ts: Iterable<T>) = use { ts.forEach { t -> report(t) } }

  @Synchronized
  override fun reportVerificationResult(pluginVerificationResult: PluginVerificationResult) {
    with(pluginVerificationResult) {
      val verificationTargetDirectory = targetDirectoryProvider(verificationTarget)
      val directory = verificationTargetDirectory
        .resolve("plugins")
        .resolve(createPluginVerificationDirectory(plugin))

      reportVerificationDetails(directory, "verification-verdict.txt", listOf(pluginVerificationResult)) { it.verificationVerdict }

      return when (this) {
        is PluginVerificationResult.Verified -> {
          reportVerificationDetails(directory, "compatibility-warnings.txt", compatibilityWarnings)
          reportVerificationDetails(directory, "compatibility-problems.txt", compatibilityProblems)
          reportVerificationDetails(directory, "dependencies.txt", listOf(dependenciesGraph)) { DependenciesGraphPrettyPrinter(it).prettyPresentation() }
          reportVerificationDetails(directory, "deprecated-usages.txt", deprecatedUsages)
          reportVerificationDetails(directory, "experimental-api-usages.txt", experimentalApiUsages)
          reportVerificationDetails(directory, "internal-api-usages.txt", internalApiUsages)
          reportVerificationDetails(directory, "override-only-usages.txt", overrideOnlyMethodUsages)
          reportVerificationDetails(directory, "non-extendable-api-usages.txt", nonExtendableApiUsages)
          reportVerificationDetails(directory, "plugin-structure-warnings.txt", pluginStructureWarnings)

          val problemIgnoredEvents = ignoredProblems.map { ProblemIgnoredEvent(plugin, verificationTarget, it.key, it.value) }
          problemIgnoredEvents.forEach { allIgnoredProblemsReporter.report(it) }
          IgnoredProblemsReporter(directory, verificationTarget).useReporter(problemIgnoredEvents)
          pluginVerificationReportageResultAggregator.handleVerificationResult(this, verificationTargetDirectory)
        }
        is PluginVerificationResult.InvalidPlugin -> {
          reportVerificationDetails(directory, "invalid-plugin.txt", pluginStructureErrors)
          pluginVerificationReportageResultAggregator.handleVerificationResult(this, verificationTargetDirectory)
        }
        is PluginVerificationResult.NotFound -> Unit
        is PluginVerificationResult.FailedToDownload -> Unit
      }
    }
  }

  private fun <T> reportVerificationDetails(
    directory: Path,
    fileName: String,
    content: Iterable<T>,
    lineProvider: (T) -> String = { it.toString() }
  ) {
    FileReporter(directory.resolve(fileName), lineProvider).useReporter(content)
  }
}