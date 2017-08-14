package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.api.Progress
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.tasks.Task
import com.jetbrains.pluginverifier.tasks.TaskParameters
import com.jetbrains.pluginverifier.tasks.TaskParametersBuilder
import com.jetbrains.pluginverifier.tasks.TaskResult
import org.slf4j.LoggerFactory

/**
 * @author Sergey Patrikeev
 */
abstract class TaskRunner {

  private val LOG = LoggerFactory.getLogger(TaskRunner::class.java)

  abstract val commandName: String

  protected abstract fun getParametersBuilder(): TaskParametersBuilder

  protected abstract fun createTask(parameters: TaskParameters): Task

  fun runTask(opts: CmdOpts, freeArgs: List<String>, progress: Progress): TaskResult =
      getParametersBuilder().build(opts, freeArgs).use { parameters ->
        LOG.info("Task $commandName parameters: $parameters")
        val task = createTask(parameters)
        task.execute(progress)
      }

}