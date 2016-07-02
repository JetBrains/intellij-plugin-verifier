package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.commands.VerifierCommand
import com.jetbrains.pluginverifier.utils.CommandHolder
import com.jetbrains.pluginverifier.utils.Util
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.GnuParser
import org.apache.commons.cli.ParseException
import java.util.*

object PluginVerifierMain {

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

    var freeArgs = Arrays.asList(*commandLine.args)

    var command: VerifierCommand?
    if (freeArgs.isEmpty()) {
      command = CommandHolder.getDefaultCommand()
    } else {
      command = CommandHolder.getCommand(freeArgs[0])
      if (command == null) {
        command = CommandHolder.getDefaultCommand()
      } else {
        freeArgs = freeArgs.subList(1, freeArgs.size)
      }
    }

    val exitCode = command!!.execute(commandLine, freeArgs)

    if (exitCode != 0 && !java.lang.Boolean.getBoolean("exitCode0")) {
      System.exit(exitCode)
    }
  }

}
