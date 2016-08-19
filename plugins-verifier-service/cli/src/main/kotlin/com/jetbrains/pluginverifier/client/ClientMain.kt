package com.jetbrains.pluginverifier.client

import com.jetbrains.pluginverifier.client.commands.*
import com.jetbrains.pluginverifier.utils.CmdOpts
import com.sampullara.cli.Args
import com.sampullara.cli.Argument
import org.jetbrains.plugins.verifier.service.params.JdkVersion
import org.slf4j.LoggerFactory

/**
 * @author Sergey Patrikeev
 */
class Client {

  companion object {

    private val COMMANDS = arrayOf(GetResultCommand(), CancelTaskCommand(), CheckIdeCommand(), CheckPluginCommand(), CheckRangeCommand(), CheckTrunkApiCommand())

    private val LOG = LoggerFactory.getLogger(Client::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
      val opts = BaseCmdOpts()
      val freeArgs = Args.parse(opts, args, false)
      if (freeArgs.isEmpty()) {
        throw IllegalArgumentException("Command is not specified")
      }
      val commandName = freeArgs[0]

      LOG.debug("Debug mode ON")

      val command = COMMANDS.find { it.name().equals(commandName) } ?: throw IllegalArgumentException("Command $commandName is unknown")
      command.execute(opts, freeArgs.drop(1))
    }

  }


}


open class BaseCmdOpts : CmdOpts() {
  @set:Argument("host", description = "Verifier service host")
  var host = "http://localhost:8080"

  @set:Argument("jdk", description = "The Oracle JDK version with which to check the plugins (either 6, 7 or 8)")
  var jdkVersion: Int = 8

  companion object {
    fun parseJdkVersion(opts: BaseCmdOpts): JdkVersion? {
      val jdkVersion: JdkVersion = when (opts.jdkVersion) {
        6 -> JdkVersion.JAVA_6_ORACLE
        7 -> JdkVersion.JAVA_7_ORACLE
        8 -> JdkVersion.JAVA_8_ORACLE
        else -> {
          throw IllegalArgumentException("Unsupported JDK version ${opts.jdkVersion}")
        }
      }
      return jdkVersion
    }


  }

}