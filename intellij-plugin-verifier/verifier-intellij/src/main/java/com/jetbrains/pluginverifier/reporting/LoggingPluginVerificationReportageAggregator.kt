package com.jetbrains.pluginverifier.reporting

import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.reporting.common.LogReporter
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.absolute

/**
 * Stateful aggregator verification reports and their corresponding directories.
 * Allows aggregating such reports and logging them to a logger.
 */
class LoggingPluginVerificationReportageAggregator(
  private val messageReporters: List<LogReporter<String>> = listOf(LogReporter(LoggerFactory.getLogger("verification")))
) : PluginVerificationReportageAggregator {

  private val resultsInDirectories = mutableListOf<Pair<PluginVerificationResult, Path>>()

  override fun handleVerificationResult(result: PluginVerificationResult, targetDirectory: Path) {
    resultsInDirectories.add(result to targetDirectory)
  }

  fun handleAggregatedReportage() {
    messageReporters.forEach { reporter ->
      for ((result, targetDirectory) in resultsInDirectories) {
        val message = "Verification reports for " + result.plugin + " saved to ${targetDirectory.absolute()}"
        reporter.report(message)
      }
    }
  }
}