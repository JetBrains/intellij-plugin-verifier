package com.jetbrains.pluginverifier.client.commands

import com.jetbrains.pluginverifier.client.BaseCmdOpts
import com.jetbrains.pluginverifier.client.network.VerifierService
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import org.jetbrains.plugins.verifier.service.api.TaskId
import org.slf4j.LoggerFactory

/**
 * @author Sergey Patrikeev
 */
class CancelTaskCommand : Command {
  override fun name(): String = "cancel-task"

  companion object {
    private val LOG = LoggerFactory.getLogger(CancelTaskCommand::class.java)
  }

  override fun execute(opts: BaseCmdOpts, freeArgs: List<String>) {
    val options = CancelTaskOptions()
    Args.parseOrExit(options, freeArgs.toTypedArray())
    val service = VerifierService(options.host)
    val call = service.taskResultsService.cancelTask(TaskId(Integer.parseInt(options.taskId)))
    val response = call.execute()
    LOG.info("Cancellation result: ${response.body().string()}")
  }

}

class CancelTaskOptions() : BaseCmdOpts() {
  @set:Argument("task-id", required = true, description = "The task id to be canceled")
  var taskId: String = ""
}

