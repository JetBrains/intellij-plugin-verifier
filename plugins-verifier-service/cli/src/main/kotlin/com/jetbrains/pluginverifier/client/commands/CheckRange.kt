package com.jetbrains.pluginverifier.client.commands

import com.jetbrains.pluginverifier.api.VOptions
import com.jetbrains.pluginverifier.client.BaseCmdOpts
import com.jetbrains.pluginverifier.configurations.CheckRangeResults
import com.jetbrains.pluginverifier.output.StreamVPrinter
import com.jetbrains.pluginverifier.utils.VOptionsUtil
import org.jetbrains.plugins.verifier.service.api.CheckRange
import org.jetbrains.plugins.verifier.service.params.CheckRangeRunnerParams
import org.jetbrains.plugins.verifier.service.params.JdkVersion
import java.io.File

/**
 * @author Sergey Patrikeev
 */
class CheckRangeCommand : Command {
  override fun name(): String = "check-range"

  override fun execute(opts: BaseCmdOpts, freeArgs: List<String>) {
    if (freeArgs.isEmpty()) {
      throw IllegalArgumentException("The plugin is not specified")
    }

    val jdkVersion = BaseCmdOpts.parseJdkVersion(opts) ?: throw IllegalArgumentException("Specify the JDK version to check with")
    val vOptions = VOptionsUtil.parseOpts(opts)
    val runnerParams = createRunnerParams(jdkVersion, vOptions)
    val results = CheckRange(opts.host, File(freeArgs[0]), runnerParams).execute()
    processResults(opts, results)
  }

  private fun createRunnerParams(jdkVersion: JdkVersion, vOptions: VOptions) = CheckRangeRunnerParams(jdkVersion, vOptions)

  fun processResults(opts: BaseCmdOpts, results: CheckRangeResults) {
    StreamVPrinter(System.out).printResults(results.vResults!!, VOptionsUtil.parsePrinterOptions(opts))
  }

}