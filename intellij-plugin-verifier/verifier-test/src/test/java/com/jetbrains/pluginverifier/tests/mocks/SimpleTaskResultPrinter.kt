package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.tasks.TaskResult
import com.jetbrains.pluginverifier.tasks.TaskResultPrinter

class SimpleTaskResultPrinter : TaskResultPrinter {
  private val taskResults : List<TaskResult> = mutableListOf()

  override fun printResults(taskResult: TaskResult, outputOptions: OutputOptions) {
    taskResults + taskResult
  }
}