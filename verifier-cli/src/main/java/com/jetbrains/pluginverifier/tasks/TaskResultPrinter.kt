package com.jetbrains.pluginverifier.tasks

/**
 * @author Sergey Patrikeev
 */
interface TaskResultPrinter {
  fun printResults(taskResult: TaskResult)
}