package com.jetbrains.pluginverifier

import com.jetbrains.pluginverifier.configurations.Configuration
import com.jetbrains.pluginverifier.configurations.ConfigurationParams
import com.jetbrains.pluginverifier.configurations.ConfigurationParamsBuilder
import com.jetbrains.pluginverifier.configurations.ConfigurationResults
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.options.PublicOpts
import com.sampullara.cli.Args
import org.slf4j.LoggerFactory

object PluginVerifierMain {

  private val LOG = LoggerFactory.getLogger(PluginVerifierMain.javaClass)

  private val runners = listOf(CheckPluginRunner(), CheckIdeRunner(), CheckTrunkApiRunner())

  @JvmStatic fun main(args: Array<String>) {
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

    val runner = findRunner(command)
    val paramsParser = runner.getParamsParser()
    val parameters = paramsParser.build(opts, freeArgs)
    parameters.use {
      LOG.info("Verification parameters: $parameters")
      @Suppress("UNCHECKED_CAST")
      val configuration = runner.getConfiguration(parameters) as Configuration<ConfigurationParams, ConfigurationResults>
      val results = configuration.execute()
      val printerOptions = OptionsParser.parsePrinterOptions(opts)
      results.printResults(printerOptions)
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun findRunner(command: String?) =
      runners.find { command == it.commandName } as? ConfigurationRunner<ConfigurationParams, ConfigurationParamsBuilder<ConfigurationParams>, ConfigurationResults, *>
          ?: throw IllegalArgumentException("Unsupported command: $command. Supported commands: ${runners.map { it.commandName }}")


}
