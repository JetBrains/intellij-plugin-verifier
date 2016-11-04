package com.jetbrains.pluginverifier.client.commands

import com.intellij.structure.domain.IdeManager
import com.jetbrains.pluginverifier.client.BaseCmdOpts
import com.jetbrains.pluginverifier.configurations.CheckPluginParamsParser
import com.jetbrains.pluginverifier.configurations.CheckPluginResults
import com.jetbrains.pluginverifier.output.TeamCityVPrinter
import com.jetbrains.pluginverifier.utils.VOptionsUtil
import org.jetbrains.plugins.verifier.service.api.CheckPlugin
import org.jetbrains.plugins.verifier.service.params.CheckPluginRunnerParams
import org.jetbrains.plugins.verifier.service.params.JdkVersion
import org.slf4j.LoggerFactory
import java.io.File

/**
 * @author Sergey Patrikeev
 */
class CheckPluginCommand : Command {
  override fun name(): String = "check-plugin"

  companion object {
    private val LOG = LoggerFactory.getLogger(CheckPluginCommand::class.java)
  }

  override fun execute(opts: BaseCmdOpts, freeArgs: List<String>) {
    if (freeArgs.size <= 1) {
      throw IllegalArgumentException("You have to specify the plugin to check and IDE(s)")
    }
    val ideFiles = freeArgs.drop(1).map { File(it) }
    ideFiles.forEach { require(it.isDirectory, { "The IDE must be a directory" }) }

    val pluginToTestArg = freeArgs[0]

    val ides = ideFiles.map { IdeManager.getInstance().createIde(it) }
    val pluginFiles = CheckPluginParamsParser.getPluginFiles(pluginToTestArg, ides.map { it.version })

    val runnerParams = createRunnerParams(opts)

    try {
      val results = CheckPlugin(opts.host, ideFiles, pluginFiles.map { it.getFile() }, runnerParams).execute()
      processResults(results, opts)
    } finally {
      pluginFiles.forEach { it.release() }
    }
  }


  fun processResults(results: CheckPluginResults, opts: BaseCmdOpts) {
    if (opts.needTeamCityLog) {
      results.printTcLog(TeamCityVPrinter.GroupBy.parse(opts.group), true, VOptionsUtil.parsePrinterOptions(opts))
    }
  }


  private fun createRunnerParams(opts: BaseCmdOpts): CheckPluginRunnerParams {
    val jdkVersion: JdkVersion = BaseCmdOpts.parseJdkVersion(opts) ?: throw IllegalArgumentException("Specify the JDK version to check with")
    val vOptions = VOptionsUtil.parseOpts(opts)
    return CheckPluginRunnerParams(jdkVersion, vOptions)
  }

}