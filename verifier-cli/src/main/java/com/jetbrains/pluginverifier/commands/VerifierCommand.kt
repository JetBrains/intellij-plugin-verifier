package com.jetbrains.pluginverifier.commands

import com.jetbrains.pluginverifier.utils.Opts

abstract class VerifierCommand(val name: String) {

  /**
   * @return exit code
   */
  @Throws(Exception::class)
  abstract fun execute(opts: Opts, freeArgs: List<String>): Int

}
