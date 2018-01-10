package com.jetbrains.pluginverifier.tasks

/**
 * Implementations of this interface print
 * the verification [results] [TaskResult]
 * in a way specific for a concrete [Task].
 */
interface TaskResultPrinter {
  fun printResults(taskResult: TaskResult)
}