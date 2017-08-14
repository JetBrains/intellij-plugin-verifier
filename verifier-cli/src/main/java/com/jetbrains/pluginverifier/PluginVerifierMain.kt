package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.api.DefaultProgress
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.options.PublicOpts
import com.sampullara.cli.Args

object PluginVerifierMain {

  private val taskRunners: List<TaskRunner> = listOf(CheckPluginRunner(), CheckIdeRunner(), CheckTrunkApiRunner())

  @JvmStatic
  fun main(args: Array<String>) {
    val opts = CmdOpts()
    var freeArgs = Args.parse(opts, args, false)

    if (freeArgs.isEmpty()) {
      System.err.println("""The command is not specified. Should be one of 'check-plugin' or 'check-ide'.
  Example: java -jar verifier.jar -r /usr/lib/jvm/java-8-oracle check-plugin /tmp/Kotlin /tmp/IU-162.2032.8 /tmp/IU-163.1024 /tmp/IU-163.7277
        OR java -jar verifier.jar -html-report report.html check-ide /tmp/IU-162.2032.8

  More examples on https://github.com/JetBrains/intellij-plugin-verifier/
""")
      Args.usage(System.err, PublicOpts())

      System.exit(1)
    }

    val command = freeArgs[0]
    freeArgs = freeArgs.drop(1)

    val taskRunner = findTaskRunner(command)
    val taskResult = taskRunner.runTask(opts, freeArgs, DefaultProgress())
    val printerOptions = OptionsParser.parsePrinterOptions(opts)
    taskResult.printResults(printerOptions)
  }

  private fun findTaskRunner(command: String?) = taskRunners.find { command == it.commandName }
      ?: throw IllegalArgumentException("Unsupported command: $command. Supported commands: ${taskRunners.map { it.commandName }}")

}
