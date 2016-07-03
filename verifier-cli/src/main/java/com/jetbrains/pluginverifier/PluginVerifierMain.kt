package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.commands.CheckIdeCommand
import com.jetbrains.pluginverifier.commands.CheckPluginCommand
import com.jetbrains.pluginverifier.commands.NewProblemsCommand
import com.jetbrains.pluginverifier.commands.VerifierCommand
import com.jetbrains.pluginverifier.utils.Opts
import com.sampullara.cli.Args

object PluginVerifierMain {

  val COMMAND_MAP = listOf<VerifierCommand>(CheckIdeCommand(), NewProblemsCommand(), CheckPluginCommand()).associateBy { it.name }

  @JvmStatic fun main(args: Array<String>) {
    val opts = Opts()
    val freeArgs = Args.parse(opts, args)

    if (args.isEmpty()) {
      System.err.println("The command is not specified. Should be one of ${COMMAND_MAP.keys.joinToString()}")
      System.exit(1)
    }

    val command = args[0]

    val verifierCommand = COMMAND_MAP[command]
    if (verifierCommand == null) {
      System.err.println("Unknown command $command. Should be one of ${COMMAND_MAP.keys.joinToString()}")
      System.exit(1)
    }
    val time = System.currentTimeMillis()
    val exitCode = verifierCommand!!.execute(opts, freeArgs.subList(1, freeArgs.size).toList())
    println("Completed in ${(System.currentTimeMillis() - time) / 1000} seconds")

    //TODO: replace exitCode0 with cli-parameter
    if (exitCode != 0 && !java.lang.Boolean.getBoolean("exitCode0")) {
      System.exit(exitCode)
    }
  }

}
