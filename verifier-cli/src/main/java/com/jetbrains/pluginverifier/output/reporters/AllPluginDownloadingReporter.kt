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
    val distinctReports = collectingReporter.getReported().distinctBy { it.pluginInfo }

    var totalDownloadDuration = Duration.ZERO
    var totalDownloaded = SpaceAmount.ZERO_SPACE
    var totalSpaceUsed = SpaceAmount.ZERO_SPACE

    for (report in distinctReports) {
      totalSpaceUsed += report.pluginSize
      if (report.downloadDuration != Duration.ZERO) {
        totalDownloadDuration += report.downloadDuration
        totalDownloaded += report.pluginSize
      }
    }

    verificationLogger.info("Total time spent downloading plugins and their dependencies: ${totalDownloadDuration.formatDuration()}")
    verificationLogger.info("Total amount of plugins and dependencies downloaded: ${totalDownloaded.presentableAmount()}")
    verificationLogger.info("Total amount of space used for plugins and dependencies: ${totalSpaceUsed.presentableAmount()}")
    if (outputOptions.teamCityLog != null) {
      outputOptions.teamCityLog.buildStatisticValue("intellij.plugin.verifier.downloading.time.ms", totalDownloadDuration.toMillis())
      outputOptions.teamCityLog.buildStatisticValue("intellij.plugin.verifier.downloading.amount.bytes", totalDownloaded.to(SpaceUnit.BYTE).toLong())
      outputOptions.teamCityLog.buildStatisticValue("intellij.plugin.verifier.total.space.used", totalSpaceUsed.to(SpaceUnit.BYTE).toLong())
    }
  }

}