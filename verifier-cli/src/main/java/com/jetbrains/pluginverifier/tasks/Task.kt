package com.jetbrains.pluginverifier.tasks

import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage

abstract class Task {
  abstract fun execute(verificationReportage: VerificationReportage): TaskResult
}
