package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.commands.CheckIdeCommand
import com.jetbrains.pluginverifier.commands.CheckPluginCommand
import com.jetbrains.pluginverifier.commands.NewProblemsCommand
import com.jetbrains.pluginverifier.commands.VerifierCommand
import com.jetbrains.pluginverifier.utils.Util
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.GnuParser
import org.apache.commons.cli.ParseException

object PluginVerifierMain {

  val COMMAND_MAP = listOf<VerifierCommand>(CheckIdeCommand(), NewProblemsCommand(), CheckPluginCommand()).associateBy { it.name }

  @JvmStatic fun main(args: Array<String>) {
    val commandLine: CommandLine
    try {
      commandLine = GnuParser().parse(Util.CMD_OPTIONS, args)
    } catch (e: ParseException) {
      throw RuntimeException(e)
    }

    if (commandLine.hasOption('h')) {
      Util.printHelp()
      return
    }

    if (args.isEmpty()) {
      System.err.println("The command is not specified. Should be one of ${COMMAND_MAP.keys.joinToString()}")
      System.exit(1)
    }

    val command = args[0]
    val params = args.copyOfRange(1, args.size)

    val verifierCommand = COMMAND_MAP[command]
    if (verifierCommand == null) {
      System.err.println("Unknown command $command. Should be one of ${COMMAND_MAP.keys.joinToString()}")
      System.exit(1)
    }
    val exitCode = verifierCommand!!.execute(commandLine, params.toList())

    //TODO: replace exitCode0 with cli-parameter
    if (exitCode != 0 && !java.lang.Boolean.getBoolean("exitCode0")) {
      System.exit(exitCode)
    }
  }

}
