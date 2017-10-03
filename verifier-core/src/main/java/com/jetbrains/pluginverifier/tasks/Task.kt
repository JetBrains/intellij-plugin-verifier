package com.jetbrains.pluginverifier.tasks

import com.jetbrains.pluginverifier.logging.VerificationLogger

abstract class Task {
  abstract fun execute(logger: VerificationLogger): TaskResult
}
