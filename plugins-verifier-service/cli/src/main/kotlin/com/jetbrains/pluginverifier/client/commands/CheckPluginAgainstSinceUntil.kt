package com.jetbrains.pluginverifier.client.commands

import com.jetbrains.pluginverifier.api.VOptions
import com.jetbrains.pluginverifier.client.BaseCmdOpts
import com.jetbrains.pluginverifier.utils.VOptionsUtil
import org.jetbrains.plugins.verifier.service.api.CheckPluginAgainstSinceUntil
import org.jetbrains.plugins.verifier.service.params.CheckPluginAgainstSinceUntilBuildsRunnerParams
import org.jetbrains.plugins.verifier.service.params.JdkVersion
import org.jetbrains.plugins.verifier.service.results.CheckPluginAgainstSinceUntilBuildsResults
import java.io.File

/**
 * @author Sergey Patrikeev
 */
class CheckPluginAgainstSinceUntilCommand : Command {
  override fun name(): String = "check-plugin-against-since-until-builds"

  override fun execute(opts: BaseCmdOpts, freeArgs: List<String>) {
    if (freeArgs.isEmpty()) {
      throw IllegalArgumentException("The plugin is not specified")
    }

    val jdkVersion = BaseCmdOpts.parseJdkVersion(opts) ?: throw IllegalArgumentException("Specify the JDK version to check with")
    val vOptions = VOptionsUtil.parseOpts(opts)
    val runnerParams = createRunnerParams(jdkVersion, vOptions)
    val results = CheckPluginAgainstSinceUntil(opts.host, File(freeArgs[0]), runnerParams).execute()
    processResults(results)
  }

  private fun createRunnerParams(jdkVersion: JdkVersion, vOptions: VOptions) = CheckPluginAgainstSinceUntilBuildsRunnerParams(jdkVersion, vOptions)

  fun processResults(results: CheckPluginAgainstSinceUntilBuildsResults) {
    results.printResults(System.out)
  }

}