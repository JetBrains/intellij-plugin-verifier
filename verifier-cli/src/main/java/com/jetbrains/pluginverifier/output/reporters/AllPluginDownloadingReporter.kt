package com.jetbrains.pluginverifier.output.reporters

import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.misc.formatDuration
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.reporting.Reporter
import com.jetbrains.pluginverifier.reporting.common.CollectingReporter
import com.jetbrains.pluginverifier.reporting.downloading.PluginDownloadReport
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.cleanup.SpaceUnit
import org.slf4j.Logger
import java.time.Duration

/**
 * Counts total time spent on downloading plugins and their dependencies
 * and total amount of bytes downloaded.
 */
class AllPluginDownloadingReporter(
    private val outputOptions: OutputOptions,
    private val verificationLogger: Logger
) : Reporter<PluginDownloadReport> {

  private val collectingReporter = CollectingReporter<PluginDownloadReport>()

  override fun report(t: PluginDownloadReport) {
    collectingReporter.report(t)
  }

  override fun close() {
    try {
      reportDownloadingStatistics()
    } finally {
      collectingReporter.closeLogged()
    }
  }

  private fun reportDownloadingStatistics() {
    var totalDownloadDuration = Duration.ZERO
    var totalDownloadedAmount = SpaceAmount.ZERO_SPACE

    for (report in collectingReporter.allReported) {
      if (report.downloadDuration != Duration.ZERO) {
        totalDownloadDuration += report.downloadDuration
        totalDownloadedAmount += report.pluginSize
      }
    }

    val totalSpaceUsed = collectingReporter.allReported.distinctBy { it.pluginInfo }
        .fold(SpaceAmount.ZERO_SPACE) { acc, report -> acc + report.pluginSize }

    verificationLogger.info("Total time spent downloading plugins and their dependencies: ${totalDownloadDuration.formatDuration()}")
    verificationLogger.info("Total amount of plugins and dependencies downloaded: ${totalDownloadedAmount.presentableAmount()}")
    verificationLogger.info("Total amount of space used for plugins and dependencies: ${totalSpaceUsed.presentableAmount()}")
    if (outputOptions.teamCityLog != null) {
      outputOptions.teamCityLog.buildStatisticValue("intellij.plugin.verifier.downloading.time.ms", totalDownloadDuration.toMillis())
      outputOptions.teamCityLog.buildStatisticValue("intellij.plugin.verifier.downloading.amount.bytes", totalDownloadedAmount.to(SpaceUnit.BYTE).toLong())
      outputOptions.teamCityLog.buildStatisticValue("intellij.plugin.verifier.total.space.used", totalSpaceUsed.to(SpaceUnit.BYTE).toLong())
    }
  }

}