package com.jetbrains.pluginverifier.tasks

import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage

/**
 * Interface of all the verification tasks.
 *
 * Implementations of this interface accept
 * the [TaskParameters] in constructors.
 */
interface Task {
  /**
   * Runs the verification task according to the [parameters] [TaskParameters]
   * passed to the [Task]'s constructor.
   *
   * The [verificationReportage] is used to log the
   * verification stages and results in a configurable way.
   */
  fun execute(verificationReportage: VerificationReportage): TaskResult
}
