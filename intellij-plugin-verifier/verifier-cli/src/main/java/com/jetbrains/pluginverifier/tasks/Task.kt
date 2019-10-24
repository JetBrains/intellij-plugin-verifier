package com.jetbrains.pluginverifier.tasks

import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage

/**
 * Interface of all the verifier tasks.
 */
interface Task {
  /**
   * Runs the task.
   */
  fun execute(
    reportage: PluginVerificationReportage,
    pluginDetailsCache: PluginDetailsCache
  ): TaskResult
}
