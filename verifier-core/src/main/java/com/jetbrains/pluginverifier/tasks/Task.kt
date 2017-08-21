package com.jetbrains.pluginverifier.tasks

import com.jetbrains.pluginverifier.api.Progress

abstract class Task {
  abstract fun execute(progress: Progress): TaskResult
}
