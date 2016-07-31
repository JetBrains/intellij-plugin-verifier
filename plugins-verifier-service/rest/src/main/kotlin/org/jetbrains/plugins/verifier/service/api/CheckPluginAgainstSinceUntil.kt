package org.jetbrains.plugins.verifier.service.api

import org.jetbrains.plugins.verifier.service.client.MultipartUtil
import org.jetbrains.plugins.verifier.service.client.executeSuccessfully
import org.jetbrains.plugins.verifier.service.client.parseTaskId
import org.jetbrains.plugins.verifier.service.client.util.ArchiverUtil
import org.jetbrains.plugins.verifier.service.client.waitCompletion
import org.jetbrains.plugins.verifier.service.params.CheckPluginAgainstSinceUntilBuildsRunnerParams
import org.jetbrains.plugins.verifier.service.results.CheckPluginAgainstSinceUntilBuildsResults
import org.jetbrains.plugins.verifier.service.util.deleteLogged
import org.slf4j.LoggerFactory
import java.io.File

class CheckPluginAgainstSinceUntil(host: String,
                                   val pluginFile: File,
                                   val runnerParams: CheckPluginAgainstSinceUntilBuildsRunnerParams) : VerifierServiceApi<CheckPluginAgainstSinceUntilBuildsResults>(host) {

  companion object {
    private val LOG = LoggerFactory.getLogger(CheckPluginAgainstSinceUntil::class.java)
  }

  override fun executeImpl(): CheckPluginAgainstSinceUntilBuildsResults {
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
      return doCheck(pf)
    } finally {
      if (delete) {
        pf.deleteLogged()
      }
    }
  }

  private fun doCheck(pluginFile: File): CheckPluginAgainstSinceUntilBuildsResults {

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