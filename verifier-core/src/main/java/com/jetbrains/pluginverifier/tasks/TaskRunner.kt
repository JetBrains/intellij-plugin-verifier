package com.jetbrains.pluginverifier.tasks

import com.jetbrains.pluginverifier.plugin.PluginCreator
import com.jetbrains.pluginverifier.repository.IdeRepository
import com.jetbrains.pluginverifier.repository.PluginRepository

/**
 * @author Sergey Patrikeev
 */
abstract class TaskRunner {

  abstract val commandName: String

  abstract fun getParametersBuilder(pluginRepository: PluginRepository, ideRepository: IdeRepository, pluginCreator: PluginCreator): TaskParametersBuilder

  abstract fun createTask(parameters: TaskParameters, pluginRepository: PluginRepository, pluginCreator: PluginCreator): Task

}