package com.jetbrains.pluginverifier.commands

import org.apache.commons.cli.CommandLine

abstract class VerifierCommand(val name: String) {

  /**
   * @return exit code
   */
  @Throws(Exception::class)
  abstract fun execute(commandLine: CommandLine, freeArgs: List<String>): Int

}
