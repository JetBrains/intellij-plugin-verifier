package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.configurations.Task
import com.jetbrains.pluginverifier.configurations.TaskParameters
import com.jetbrains.pluginverifier.configurations.TaskParametersBuilder
import com.jetbrains.pluginverifier.configurations.TaskResult

/**
 * @author Sergey Patrikeev
 */
abstract class TaskRunner<Params : TaskParameters, out ParamsParser : TaskParametersBuilder<Params>, out Result : TaskResult, out Conf : Task<Params, Result>> {

  abstract val commandName: String

  abstract fun getParamsParser(): ParamsParser

  abstract fun getTask(parameters: Params): Conf

}