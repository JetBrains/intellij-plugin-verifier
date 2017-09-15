package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.api.Progress
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.plugin.PluginCreator
import com.jetbrains.pluginverifier.repository.IdeRepository
import com.jetbrains.pluginverifier.repository.PluginRepository
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

  protected abstract fun getParametersBuilder(pluginRepository: PluginRepository, ideRepository: IdeRepository, pluginCreator: PluginCreator): TaskParametersBuilder

  protected abstract fun createTask(parameters: TaskParameters, pluginRepository: PluginRepository, pluginCreator: PluginCreator): Task

  fun runTask(
      opts: CmdOpts,
      freeArgs: List<String>,
      pluginRepository: PluginRepository,
      ideRepository: IdeRepository,
      pluginCreator: PluginCreator,
      progress: Progress
  ): TaskResult {
    val parametersBuilder = getParametersBuilder(pluginRepository, ideRepository, pluginCreator)
    val parameters = parametersBuilder.build(opts, freeArgs)
    return parameters.use {
      LOG.info("Task $commandName parameters: $parameters")
      val task = createTask(parameters, pluginRepository, pluginCreator)
      task.execute(progress)
    }
  }

}