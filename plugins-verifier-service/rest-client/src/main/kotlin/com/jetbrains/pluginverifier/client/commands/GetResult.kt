package com.jetbrains.pluginverifier.client.commands

import com.jetbrains.pluginverifier.client.BaseCmdOpts
import com.jetbrains.pluginverifier.client.network.VerifierService
import com.jetbrains.pluginverifier.client.network.waitCompletion
import com.jetbrains.pluginverifier.configurations.CheckIdeResults
import com.jetbrains.pluginverifier.configurations.CheckPluginResults
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import org.jetbrains.plugins.verifier.service.api.TaskId
import org.jetbrains.plugins.verifier.service.results.CheckPluginAgainstSinceUntilBuildsResults
import org.jetbrains.plugins.verifier.service.results.CheckTrunkApiResults

/**
 * @author Sergey Patrikeev
 */
class GetResultCommand : Command {

  override fun name(): String = "get-result"

  override fun execute(opts: BaseCmdOpts, freeArgs: List<String>) {
    val options = GetTaskResultOptions()
    val free = Args.parse(options, freeArgs.toTypedArray())
    val service = VerifierService(opts.host)
    val taskId = TaskId(Integer.parseInt(options.taskId))
    if (free.isEmpty()) {
      throw IllegalArgumentException("Check result command is not specified")
    }
    when (free[0]) {
      "check-ide" -> {
        val result = waitCompletion<CheckIdeResults>(service, taskId)
        CheckIdeCommand().processResults(result, options, service)
      }
      "check-plugin" -> {
        val result = waitCompletion<CheckPluginResults>(service, taskId)
        CheckPluginCommand().processResults(opts, result)
      }
      "check-since-until" -> {
        val result = waitCompletion<CheckPluginAgainstSinceUntilBuildsResults>(service, taskId)
        CheckPluginAgainstSinceUntilCommand().processResults(result)
      }
      "check-trunk-api" -> {
        val result = waitCompletion<CheckTrunkApiResults>(service, taskId)
        CheckTrunkApiCommand().processResults(result, opts)
      }
      else -> {
        throw IllegalArgumentException("unknown command")
      }
    }

  }

  class GetTaskResultOptions() : BaseCmdOpts() {
    @set:Argument("task-id", required = true, description = "The task id to fetch result of")
    var taskId: String = "0"
  }


}

