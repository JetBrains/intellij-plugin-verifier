package com.jetbrains.pluginverifier.tasks

import com.jetbrains.pluginverifier.api.Progress

abstract class Task<Params : TaskParameters, out Results : TaskResult>(protected val parameters: Params) {
  abstract fun execute(progress: Progress): Results
}
