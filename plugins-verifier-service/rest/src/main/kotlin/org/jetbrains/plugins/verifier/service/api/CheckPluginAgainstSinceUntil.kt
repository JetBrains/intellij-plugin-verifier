package org.jetbrains.plugins.verifier.service.api

import org.jetbrains.plugins.verifier.service.client.*
import org.jetbrains.plugins.verifier.service.client.util.ArchiverUtil
import org.jetbrains.plugins.verifier.service.params.CheckPluginAgainstSinceUntilBuildsRunnerParams
import org.jetbrains.plugins.verifier.service.results.CheckPluginAgainstSinceUntilBuildsResults
import org.jetbrains.plugins.verifier.service.util.deleteLogged
import org.slf4j.LoggerFactory
import java.io.File

class CheckPluginAgainstSinceUntil(val host: String,
                                   val pluginFile: File,
                                   val runnerParams: CheckPluginAgainstSinceUntilBuildsRunnerParams) : VerifierServiceApi<CheckPluginAgainstSinceUntilBuildsResults> {

  companion object {
    private val LOG = LoggerFactory.getLogger(CheckPluginAgainstSinceUntil::class.java)
  }

  override fun execute(): CheckPluginAgainstSinceUntilBuildsResults {
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
        delete = true
      } catch (e: Exception) {
        tempFile.deleteLogged()
        throw RuntimeException("Unable to pack the plugin $pf", e)
      }
    }

    try {
      return doCheck(host, pf)
    } finally {
      if (delete) {
        pf.deleteLogged()
      }
    }
  }

  private fun doCheck(host: String,
                      pluginFile: File): CheckPluginAgainstSinceUntilBuildsResults {

    val service = VerifierService(host)

    val pluginPart = MultipartUtil.createFilePart("pluginFile", pluginFile)

    LOG.debug("The runner parameters: $runnerParams")
    val paramsPart = MultipartUtil.createJsonPart("params", runnerParams)

    val call = service.enqueueTaskService.checkPluginAgainstSinceUntilBuilds(pluginPart, paramsPart)
    val response = call.executeSuccessfully()
    val taskId = parseTaskId(response)
    LOG.info("The task ID is $taskId")

    return waitCompletion<CheckPluginAgainstSinceUntilBuildsResults>(service, taskId)
  }

}