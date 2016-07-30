package com.jetbrains.pluginverifier.client.commands

import com.jetbrains.pluginverifier.api.VOptions
import com.jetbrains.pluginverifier.client.BaseCmdOpts
import com.jetbrains.pluginverifier.client.network.*
import com.jetbrains.pluginverifier.client.util.ArchiverUtil
import com.jetbrains.pluginverifier.client.util.BaseCmdUtil
import com.jetbrains.pluginverifier.utils.VOptionsUtil
import org.jetbrains.plugins.verifier.service.params.CheckPluginAgainstSinceUntilBuildsRunnerParams
import org.jetbrains.plugins.verifier.service.params.JdkVersion
import org.jetbrains.plugins.verifier.service.results.CheckPluginAgainstSinceUntilBuildsResults
import org.jetbrains.plugins.verifier.service.util.deleteLogged
import org.slf4j.LoggerFactory
import java.io.File

/**
 * @author Sergey Patrikeev
 */
class CheckPluginAgainstSinceUntilCommand : Command {
  override fun name(): String = "check-plugin-against-since-until-builds"

  companion object {
    private val LOG = LoggerFactory.getLogger(CheckPluginAgainstSinceUntilCommand::class.java)
  }

  override fun execute(opts: BaseCmdOpts, freeArgs: List<String>) {
    if (freeArgs.isEmpty()) {
      throw IllegalArgumentException("The plugin is not specified")
    }

    val jdkVersion = BaseCmdUtil.parseJdkVersion(opts) ?: throw IllegalArgumentException("Specify the JDK version to check with")
    val vOptions = VOptionsUtil.parseOpts(opts)
    val results = checkPluginWithSinceUntilBuilds(File(freeArgs[0]), opts.host, vOptions, jdkVersion)
    results.printResults(System.out)
  }

  fun checkPluginWithSinceUntilBuilds(pluginFile: File,
                                      host: String,
                                      vOptions: VOptions,
                                      jdkVersion: JdkVersion): CheckPluginAgainstSinceUntilBuildsResults {
    var pf = pluginFile
    if (!pf.exists()) {
      throw IllegalArgumentException("The plugin file $pluginFile doesn't exist")
    }

    var delete: Boolean = false
    if (pf.isDirectory) {
      val tempFile = File.createTempFile("plugin", ".zip")
      try {
        ArchiverUtil.archiveDirectory(pf, tempFile)
        pf = tempFile
      } catch (e: Exception) {
        tempFile.deleteLogged()
        throw RuntimeException("Unable to pack the plugin $pf", e)
      }
      delete = true
    }

    try {
      return doCheck(host, pf, jdkVersion, vOptions)
    } finally {
      if (delete) {
        pf.deleteLogged()
      }
    }
  }

  private fun doCheck(host: String,
                      pluginFile: File,
                      jdkVersion: JdkVersion,
                      vOptions: VOptions): CheckPluginAgainstSinceUntilBuildsResults {

    val service = VerifierService(host)

    val pluginPart = MultipartUtil.createFilePart("pluginFile", pluginFile)
    val runnerParams = createRunnerParams(jdkVersion, vOptions)
    LOG.debug("The runner parameters: $runnerParams")
    val paramsPart = MultipartUtil.createJsonPart("params", runnerParams)

    val call = service.enqueueTaskService.checkPluginAgainstSinceUntilBuilds(pluginPart, paramsPart)
    val response = call.executeSuccessfully()
    val taskId = parseTaskId(response)
    LOG.info("The task ID is $taskId")

    val results = waitCompletion<CheckPluginAgainstSinceUntilBuildsResults>(service, taskId)
    processResults(results)
    return results
  }

  private fun createRunnerParams(jdkVersion: JdkVersion, vOptions: VOptions) = CheckPluginAgainstSinceUntilBuildsRunnerParams(jdkVersion, vOptions)

  fun processResults(results: CheckPluginAgainstSinceUntilBuildsResults) {
    results.printResults(System.out)
  }

}