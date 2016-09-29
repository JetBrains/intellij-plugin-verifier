package org.jetbrains.plugins.verifier.service.api

import com.jetbrains.pluginverifier.configurations.CheckPluginResults
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.persistence.GsonHolder
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.jetbrains.plugins.verifier.service.client.parseTaskId
import org.jetbrains.plugins.verifier.service.client.util.ArchiverUtil
import org.jetbrains.plugins.verifier.service.client.waitCompletion
import org.jetbrains.plugins.verifier.service.params.CheckPluginRunnerParams
import org.jetbrains.plugins.verifier.service.util.MediaTypes
import org.jetbrains.plugins.verifier.service.util.executeSuccessfully
import org.slf4j.LoggerFactory
import java.io.File

class CheckPlugin(host: String,
                  val ideFiles: List<File>,
                  val pluginFiles: List<File>,
                  val runnerParams: CheckPluginRunnerParams) : VerifierServiceApi<CheckPluginResults>(host) {

  companion object {
    private val LOG = LoggerFactory.getLogger(CheckIde::class.java)
  }

  override fun executeImpl(): CheckPluginResults {
    ideFiles.forEach { require(it.isDirectory, { "The IDE must be a directory" }) }

    val archivedPluginFiles: MutableList<Pair<File, Boolean>> = arrayListOf()
    try {
      pluginFiles.mapTo(archivedPluginFiles) {
        if (it.isDirectory) {
          val tempFile = File.createTempFile("plugin", ".zip")
          ArchiverUtil.archiveDirectory(it, tempFile)
          tempFile to true
        } else {
          it to false
        }
      }

      val archivedIdes: MutableList<File> = arrayListOf()
      try {
        ideFiles.mapTo(archivedIdes) {
          val tempFile = File.createTempFile("ide", ".zip")
          ArchiverUtil.archiveDirectory(it, tempFile)
          tempFile
        }

        return execute(archivedIdes, archivedPluginFiles.map { it.first })

      } finally {
        archivedIdes.forEach { it.deleteLogged() }
      }

    } finally {
      archivedPluginFiles.filter { it.second }.forEach { it.first.deleteLogged() }
    }

  }

  private fun execute(ideFiles: List<File>, pluginFiles: List<File>): CheckPluginResults {
    val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
    builder.addFormDataPart("params", null, RequestBody.create(MediaTypes.JSON, GsonHolder.GSON.toJson(runnerParams)))
    ideFiles.forEachIndexed { id, file -> builder.addFormDataPart("ide_$id", file.name, RequestBody.create(MediaTypes.OCTET_STREAM, file)) }
    pluginFiles.forEachIndexed { id, file -> builder.addFormDataPart("plugin_$id", file.name, RequestBody.create(MediaTypes.OCTET_STREAM, file)) }

    val call = service.enqueueTaskService.checkPlugin(builder.build())
    val response = call.executeSuccessfully()

    val taskId: TaskId
    taskId = parseTaskId(response)
    LOG.info("The task ID is $taskId")

    return waitCompletion(service, taskId)
  }

}