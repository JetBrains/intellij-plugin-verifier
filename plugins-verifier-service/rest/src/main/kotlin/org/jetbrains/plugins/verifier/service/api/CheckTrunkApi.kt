package org.jetbrains.plugins.verifier.service.api

import org.jetbrains.plugins.verifier.service.client.MultipartUtil
import org.jetbrains.plugins.verifier.service.client.executeSuccessfully
import org.jetbrains.plugins.verifier.service.client.parseTaskId
import org.jetbrains.plugins.verifier.service.client.util.ArchiverUtil
import org.jetbrains.plugins.verifier.service.client.waitCompletion
import org.jetbrains.plugins.verifier.service.params.CheckTrunkApiRunnerParams
import org.jetbrains.plugins.verifier.service.results.CheckTrunkApiResults
import org.jetbrains.plugins.verifier.service.util.deleteLogged
import org.slf4j.LoggerFactory
import java.io.File

class CheckTrunkApi(host: String,
                    val ideFile: File,
                    val runnerParams: CheckTrunkApiRunnerParams) : VerifierServiceApi<CheckTrunkApiResults>(host) {

  companion object {
    private val LOG = LoggerFactory.getLogger(CheckTrunkApi::class.java)
  }

  override fun executeImpl(): CheckTrunkApiResults {
    LOG.debug("The runner params: $runnerParams")

    val paramsPart = MultipartUtil.createJsonPart("params", runnerParams)
    var delete: Boolean = false

    val ideFileZipped: File
    if (ideFile.isDirectory) {
      val tempFile = File.createTempFile("ide", ".zip")
      try {
        ArchiverUtil.archiveDirectory(ideFile, tempFile)
        ideFileZipped = tempFile
        delete = true
      } catch (e: Exception) {
        tempFile.deleteLogged()
        throw RuntimeException("Unable to pack the file $ideFile", e)
      }
    } else {
      ideFileZipped = ideFile
    }

    val taskId: TaskId
    try {
      val filePart = MultipartUtil.createFilePart("ideFile", ideFile)
      val call = service.enqueueTaskService.checkTrunkApi(filePart, paramsPart)
      taskId = parseTaskId(call.executeSuccessfully())
      LOG.debug("The task ID is $taskId")
    } finally {
      if (delete) {
        ideFileZipped.deleteLogged()
      }
    }

    return waitCompletion<CheckTrunkApiResults>(service, taskId)
  }
}