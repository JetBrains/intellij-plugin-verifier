package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.configurations.CheckIdeConfiguration
import com.jetbrains.pluginverifier.configurations.CheckIdeParamsParser
import com.jetbrains.pluginverifier.configurations.CheckPluginConfiguration
import com.jetbrains.pluginverifier.configurations.CheckPluginParamsParser
import com.jetbrains.pluginverifier.utils.CmdOpts
import com.sampullara.cli.Args

object PluginVerifierMain {

  @JvmStatic fun main(args: Array<String>) {
    val opts = CmdOpts()
    var freeArgs = Args.parse(opts, args)


    if (freeArgs.isEmpty()) {
      throw IllegalArgumentException("The command is not specified. Should be one of 'check-plugin' or 'check-ide'")
    }

    val command = freeArgs[0]
    freeArgs = freeArgs.drop(1)

    when (command) {
      "check-plugin" -> {
        val params = CheckPluginParamsParser.parse(opts, freeArgs)
        CheckPluginConfiguration(params).execute().printResults(System.out)
      }
      "check-ide" -> {
        val params = CheckIdeParamsParser.parse(opts, freeArgs)
        CheckIdeConfiguration(params).execute().processResults(opts)
      }
      else -> {
        throw IllegalArgumentException("Unsupported command $command")
      }
    }
  }

}
