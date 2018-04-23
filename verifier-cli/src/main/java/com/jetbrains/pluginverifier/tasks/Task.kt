package com.jetbrains.pluginverifier.tasks

import com.jetbrains.pluginverifier.VerifierExecutor
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage

/**
 * Interface of all the verification tasks.
 *
 * Implementations of this interface accept
 * the [TaskParameters] in constructors.
 */
interface Task {
  /**
   * Runs the verification task in the given [verifierExecutor] according
   * to the [parameters] [TaskParameters] passed to the [Task]'s constructor.
   *
   * The [verificationReportage] is used to log the
   * verification stages and results in a configurable way.
   */
  fun execute(verificationReportage: VerificationReportage,
              verifierExecutor: VerifierExecutor,
              jdkDescriptorCache: JdkDescriptorsCache,
              pluginDetailsCache: PluginDetailsCache): TaskResult
}
