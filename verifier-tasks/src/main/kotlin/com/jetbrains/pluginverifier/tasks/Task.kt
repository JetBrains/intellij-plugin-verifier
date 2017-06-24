package com.jetbrains.pluginverifier.tasks

abstract class Task<Params : TaskParameters, out Results : TaskResult>(protected val parameters: Params) {
  abstract fun execute(): Results
}
