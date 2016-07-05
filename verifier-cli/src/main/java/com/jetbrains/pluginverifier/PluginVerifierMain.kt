package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.configurations.CheckIdeConfiguration
import com.jetbrains.pluginverifier.configurations.CheckIdeParamsParser
import com.jetbrains.pluginverifier.configurations.CheckPluginConfiguration
import com.jetbrains.pluginverifier.configurations.CheckPluginParamsParser
import com.jetbrains.pluginverifier.utils.Opts
import com.sampullara.cli.Args

object PluginVerifierMain {

  @JvmStatic fun main(args: Array<String>) {
    val opts = Opts()
    var freeArgs = Args.parse(opts, args)


    if (freeArgs.isEmpty()) {
      System.err.println("The command is not specified. Should be one of 'check-plugin' or 'check-ide'")
      System.exit(1)
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
        System.err.println("Unsupported command $command")
        System.exit(1)
      }
    }

    //TODO: replace exitCode0 with cli-parameter
//    if (exitCode != 0 && !java.lang.Boolean.getBoolean("exitCode0")) {
//      System.exit(exitCode)
//    }
  }

}
