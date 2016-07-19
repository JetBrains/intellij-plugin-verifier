package com.jetbrains.pluginverifier.client.commands

import com.jetbrains.pluginverifier.api.VResults
import com.jetbrains.pluginverifier.client.BaseCmdOpts
import com.jetbrains.pluginverifier.client.network.VerifierService
import com.jetbrains.pluginverifier.client.network.waitCompletion
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import org.jetbrains.plugins.verifier.service.api.TaskId

/**
 * @author Sergey Patrikeev
 */
class GetResultCommand : Command {

  override fun name(): String = "get-result"

  override fun execute(opts: BaseCmdOpts, freeArgs: List<String>) {
    val options = GetTaskResultOptions()
    Args.parseOrExit(options, freeArgs.toTypedArray())
    val result: VResults = waitCompletion(VerifierService(options.host), TaskId(Integer.parseInt(options.taskId)))
    processResult(result)
  }

  private fun processResult(result: VResults) {
    println(result)
  }


  class GetTaskResultOptions() : BaseCmdOpts() {
    @set:Argument("task-id", required = true, description = "The task id to fetch result of")
    var taskId: String = "0"
  }


}

