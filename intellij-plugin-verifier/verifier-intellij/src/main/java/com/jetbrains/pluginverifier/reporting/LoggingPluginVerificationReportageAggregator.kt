package com.jetbrains.pluginverifier.reporting

import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.reporting.common.LogReporter
import com.jetbrains.pluginverifier.repository.PluginInfo
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.io.path.absolute

/**
 * This message is parsed by the Plugin DevKit plugin.
 * If it is modified, please adjust it in the Plugin DevKit as well.
 */
private const val VERIFICATION_REPORTS = "Verification reports for %s saved to %s"

private data class ReportLocation(val plugin: PluginInfo, val targetDirectory: Path)

/**
 * Stateful aggregator verification reports and their corresponding directories.
 * Allows aggregating such reports and logging them to a logger.
 */
class LoggingPluginVerificationReportageAggregator(
  private val messageReporters: List<LogReporter<String>> = listOf(LogReporter(LoggerFactory.getLogger("verification")))
) : PluginVerificationReportageAggregator {

  private val reportLocations = ConcurrentLinkedQueue<ReportLocation>()

  override fun handleVerificationResult(result: PluginVerificationResult, targetDirectory: Path) {
    reportLocations.add(ReportLocation(result.plugin, targetDirectory))
  }

  fun handleAggregatedReportage() {
    messageReporters.forEach { reporter ->
      for ((plugin, targetDirectory) in reportLocations) {
        val message = VERIFICATION_REPORTS.format(plugin, targetDirectory.absolute())
        reporter.report(message)
      }
    }
  }
}

