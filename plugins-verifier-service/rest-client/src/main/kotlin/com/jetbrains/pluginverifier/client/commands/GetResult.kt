package com.jetbrains.pluginverifier.client.commands

import com.jetbrains.pluginverifier.client.BaseCmdOpts
import com.jetbrains.pluginverifier.configurations.CheckIdeResults
import com.jetbrains.pluginverifier.configurations.CheckPluginResults
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import org.jetbrains.plugins.verifier.service.api.GetResult
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
    if (free.isEmpty()) {
      throw IllegalArgumentException("Check result command is not specified")
    }
    val command = free[0]
    val any: Any = GetResult(options.host, Integer.parseInt(options.taskId), command).execute()
    when (command) {
      "check-ide" -> {
        CheckIdeCommand().processResults(any as CheckIdeResults, options)
      }
      "check-plugin" -> {
        CheckPluginCommand().processResults(any as CheckPluginResults, opts)
      }
      "check-since-until" -> {
        CheckPluginAgainstSinceUntilCommand().processResults(any as CheckPluginAgainstSinceUntilBuildsResults)
      }
      "check-trunk-api" -> {
        CheckTrunkApiCommand().processResults(any as CheckTrunkApiResults, opts)
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

