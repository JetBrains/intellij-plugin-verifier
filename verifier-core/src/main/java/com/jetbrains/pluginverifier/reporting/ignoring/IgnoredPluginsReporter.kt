package com.jetbrains.pluginverifier.reporting.ignoring

import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.misc.writeText
import com.jetbrains.pluginverifier.reporting.Reporter
import com.jetbrains.pluginverifier.reporting.common.CollectingReporter
import java.nio.file.Path

/**
 * Collects all [PluginIgnoredEvent]s and saves them
 * to `<verification-home>/<verification-target>/all-ignored-plugins.txt` file.
 */
class IgnoredPluginsReporter(private val verificationReportsDirectory: Path) : Reporter<PluginIgnoredEvent> {

  private val collectingReporter = CollectingReporter<PluginIgnoredEvent>()

  override fun report(t: PluginIgnoredEvent) {
    collectingReporter.report(t)
  }

  override fun close() {
    try {
      saveIgnoredPlugins()
    } finally {
      collectingReporter.closeLogged()
    }
  }

  private fun saveIgnoredPlugins() {
    val allIgnoredPlugins = collectingReporter.getReported()
    for ((verificationTarget, ignoredPlugins) in allIgnoredPlugins.groupBy { it.verificationTarget }) {
      val ignoredPluginsFile = verificationTarget
          .getReportDirectory(verificationReportsDirectory)
          .resolve("all-ignored-plugins.txt")

      ignoredPluginsFile.writeText(
          "The following plugins were excluded from the verification: \n" +
              ignoredPlugins.joinToString(separator = "\n") { "${it.pluginInfo}: ${it.reason}" }
      )
    }
  }


}