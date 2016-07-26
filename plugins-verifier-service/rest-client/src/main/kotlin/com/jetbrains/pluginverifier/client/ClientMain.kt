package com.jetbrains.pluginverifier.client

import com.jetbrains.pluginverifier.client.commands.*
import com.jetbrains.pluginverifier.utils.CmdOpts
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import org.slf4j.LoggerFactory

/**
 * @author Sergey Patrikeev
 */
class Client {

  companion object {

    private val COMMANDS = arrayOf(GetResultCommand(), CancelTaskCommand(), CheckIdeCommand(), CheckPluginCommand(), CheckPluginAgainstSinceUntilCommand(), CheckTrunkApiCommand())

    private val LOG = LoggerFactory.getLogger(Client::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
      val opts = BaseCmdOpts()
      val freeArgs = Args.parse(opts, args, false)
      if (freeArgs.isEmpty()) {
        throw IllegalArgumentException("Command is not specified")
      }
      val commandName = freeArgs[0]

      val command = COMMANDS.find { it.name().equals(commandName) } ?: throw IllegalArgumentException("Command $commandName is unknown")
      command.execute(opts, freeArgs.drop(1))
    }

  }


}


open class BaseCmdOpts : CmdOpts() {
  @set:Argument("host", description = "Verifier service host")
  var host = "http://localhost:8080"

  @set:Argument("jdk", description = "The Oracle JDK version with which to check the plugins (either 6 or 8)")
  var jdkVersion: Int? = null

  @set:Argument("compare-check-ide-reports-team-city", alias = "compare", description = "Whether to compare the check IDE report with the previous check reports and print the difference (new problems) on the TeamCity")
  var compareCheckIdeReport: Boolean = true
}