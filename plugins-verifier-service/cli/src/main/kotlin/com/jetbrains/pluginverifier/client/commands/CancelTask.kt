package com.jetbrains.pluginverifier.client.commands

import com.jetbrains.pluginverifier.client.BaseCmdOpts
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import org.jetbrains.plugins.verifier.service.api.CancelTask

/**
 * @author Sergey Patrikeev
 */
class CancelTaskCommand : Command {
  override fun name(): String = "cancel-task"

  override fun execute(opts: BaseCmdOpts, freeArgs: List<String>) {
    val options = CancelTaskOptions()
    Args.parseOrExit(options, freeArgs.toTypedArray())
    CancelTask(options.host, Integer.parseInt(options.taskId)).execute()
  }

}

class CancelTaskOptions() : BaseCmdOpts() {
  @set:Argument("task-id", required = true, description = "The task id to be canceled")
  var taskId: String = ""
}

