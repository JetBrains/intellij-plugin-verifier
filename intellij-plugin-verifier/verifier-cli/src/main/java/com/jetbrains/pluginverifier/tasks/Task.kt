package com.jetbrains.pluginverifier.tasks

import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import com.jetbrains.pluginverifier.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache

/**
 * Interface of all the verifier tasks.
 */
interface Task {
  /**
   * Runs the task.
   */
  fun execute(
      reportage: PluginVerificationReportage,
      jdkDescriptorCache: JdkDescriptorsCache,
      pluginDetailsCache: PluginDetailsCache
  ): TaskResult
}
