package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.tasks.Task
import com.jetbrains.pluginverifier.tasks.TaskParameters
import com.jetbrains.pluginverifier.tasks.TaskParametersBuilder
import com.jetbrains.pluginverifier.tasks.TaskResult

/**
 * @author Sergey Patrikeev
 */
abstract class TaskRunner<Params : TaskParameters, out ParamsParser : TaskParametersBuilder<Params>, out Result : TaskResult, out Conf : Task<Params, Result>> {

  abstract val commandName: String

  abstract fun getParamsParser(): ParamsParser

  abstract fun getTask(parameters: Params): Conf

}