/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.reporting

import com.jetbrains.plugin.structure.base.telemetry.MutablePluginTelemetry
import com.jetbrains.plugin.structure.base.telemetry.PLUGIN_ID
import com.jetbrains.plugin.structure.base.telemetry.PLUGIN_VERSION
import com.jetbrains.plugin.structure.base.telemetry.PluginTelemetry
import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.base.utils.create
import com.jetbrains.plugin.structure.base.utils.replaceInvalidFileNameCharacters
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.dependencies.ResolvedDependenciesGraph
import com.jetbrains.pluginverifier.dependencies.presentation.ResolvedDependenciesGraphPrettyPrinter
import com.jetbrains.pluginverifier.reporting.common.FileReporter
import com.jetbrains.pluginverifier.reporting.common.LogReporter
import com.jetbrains.pluginverifier.reporting.ignoring.*
import com.jetbrains.pluginverifier.reporting.telemetry.TelemetryAggregator
import com.jetbrains.pluginverifier.reporting.telemetry.toPlainString
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo
import com.jetbrains.pluginverifier.usages.internal.kotlin.KtInternalModifierUsage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


private val LOG: Logger = LoggerFactory.getLogger(DirectoryBasedPluginVerificationReportage::class.java)

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
  private val telemetryAggregator: TelemetryAggregator = TelemetryAggregator(),
  private val targetDirectoryProvider: (PluginVerificationTarget) -> Path,
  ) : PluginVerificationReportage {

  private val verificationLogger = LoggerFactory.getLogger("verification")
  private val messageReporters = listOf(LogReporter<String>(verificationLogger))
  private val ignoredPluginsReporters = listOf(IgnoredPluginsReporter(targetDirectoryProvider))
  private val allIgnoredProblemsReporter = AllIgnoredProblemsReporter(targetDirectoryProvider)

  /**
   * Guards run-wide shared components: [allIgnoredProblemsReporter]
   * and [pluginVerificationReportageResultAggregator].
   */
  private val sharedStateLock = ReentrantLock()

  /**
   * Striped locks guarding writes into a single plugin verification directory.
   */
  private val directoryLocks = Array(DIRECTORY_LOCK_STRIPES) { ReentrantLock() }

  /**
   * The [directory] is normalized to an absolute path, so the same directory always maps to the same lock,
   * even if the [targetDirectoryProvider] mixes relative and absolute paths.
   */
  private fun lockFor(directory: Path): ReentrantLock =
    directoryLocks[Math.floorMod(directory.toAbsolutePath().normalize().hashCode(), directoryLocks.size)]

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

  override fun reportTelemetry(pluginInfo: PluginInfo, telemetry: PluginTelemetry) {
    telemetryAggregator.reportTelemetry(pluginInfo, telemetry)
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

  /**
   * Writes the report files of the [pluginVerificationResult] into its plugin verification directory.
   *
   * This method is safe to call concurrently from multiple verification workers:
   * - report files of different plugins (or of different verification targets) are written in parallel;
   * - writes into the same plugin verification directory never interleave;
   * - calls into the shared [AllIgnoredProblemsReporter] and
   *   into the [PluginVerificationReportageAggregator] are serialized.
   */
  override fun reportVerificationResult(pluginVerificationResult: PluginVerificationResult) {
    with(pluginVerificationResult) {
      val verificationTargetDirectory = targetDirectoryProvider(verificationTarget)
      val directory = verificationTargetDirectory
        .resolve("plugins")
        .resolve(createPluginVerificationDirectory(plugin))

      val problemIgnoredEvents = when (this) {
        is PluginVerificationResult.Verified -> ignoredProblems.map { ProblemIgnoredEvent(plugin, verificationTarget, it.key, it.value) }
        else -> emptyList()
      }

      lockFor(directory).withLock {
        reportVerificationDetails(directory, "verification-verdict.txt", listOf(pluginVerificationResult)) { it.verificationVerdict }

        when (this) {
          is PluginVerificationResult.Verified -> {
            reportVerificationDetails(directory, "compatibility-warnings.txt", compatibilityWarnings)
            reportVerificationDetails(directory, "compatibility-problems.txt", compatibilityProblems)
            reportDependencies(directory, "dependencies.txt", dependenciesGraph)
            reportVerificationDetails(directory, "deprecated-usages.txt", deprecatedUsages)
            reportVerificationDetails(directory, "experimental-api-usages.txt", experimentalApiUsages)
            reportVerificationDetails(directory, "internal-api-usages.txt", internalApiUsages)
            reportVerificationDetails(directory, "internal-api-kt-usages.txt", kotlinInternalApiUsages)
            reportVerificationDetails(directory, "override-only-usages.txt", overrideOnlyMethodUsages)
            reportVerificationDetails(directory, "non-extendable-api-usages.txt", nonExtendableApiUsages)
            reportVerificationDetails(directory, "plugin-structure-warnings.txt", pluginStructureWarnings)
            reportVerificationDetails(directory, "telemetry.txt", telemetryAggregator[plugin].withPluginIdAndVersion(this).orEmpty()) { it.toPlainString() }
            IgnoredProblemsReporter(directory, verificationTarget).useReporter(problemIgnoredEvents)
          }
          is PluginVerificationResult.InvalidPlugin -> {
            reportVerificationDetails(directory, "invalid-plugin.txt", pluginStructureErrors)
          }
          is PluginVerificationResult.NotFound -> Unit
          is PluginVerificationResult.FailedToDownload -> Unit
        }
      }

      return when (this) {
        is PluginVerificationResult.Verified,
        is PluginVerificationResult.InvalidPlugin -> sharedStateLock.withLock {
          problemIgnoredEvents.forEach { allIgnoredProblemsReporter.report(it) }
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

  /**
   * Streams the dependencies graph directly into the [file]
   * instead of materializing a (potentially multi-megabyte) [String] first.
   */
  private fun reportDependencies(directory: Path, fileName: String, dependenciesGraph: ResolvedDependenciesGraph) {
    try {
      val dependenciesTxtPath = directory.resolve(fileName)
      Files.newBufferedWriter(dependenciesTxtPath.create()).use { writer ->
        ResolvedDependenciesGraphPrettyPrinter(dependenciesGraph).prettyPresentation(writer)
        writer.append('\n')
      }
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      LOG.error("Failed to report dependencies into $$directory (file '$fileName')", e)
    }
  }

  private val PluginVerificationResult.Verified.kotlinInternalApiUsages
    get() = internalApiUsages.filterIsInstance<KtInternalModifierUsage>()
}

private fun PluginTelemetry?.withPluginIdAndVersion(verifiedResult: PluginVerificationResult.Verified): PluginTelemetry? {
  return this?.let {
    return MutablePluginTelemetry().apply {
      merge(it)
      set(PLUGIN_ID, verifiedResult.plugin.pluginId)
      set(PLUGIN_VERSION, verifiedResult.plugin.version)
    }
  }
}

private fun PluginTelemetry?.orEmpty(): List<PluginTelemetry> {
  return if (this != null) listOf(this) else emptyList()
}

private const val DIRECTORY_LOCK_STRIPES = 64
