package com.jetbrains.pluginverifier.configurations

abstract class Task<Params : TaskParameters, out Results : TaskResult>(protected val parameters: Params) {
  abstract fun execute(): Results
}
