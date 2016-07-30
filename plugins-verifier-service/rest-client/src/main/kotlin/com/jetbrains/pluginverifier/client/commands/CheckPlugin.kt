package com.jetbrains.pluginverifier.client.commands

import com.intellij.structure.domain.IdeManager
import com.jetbrains.pluginverifier.client.BaseCmdOpts
import com.jetbrains.pluginverifier.client.network.*
import com.jetbrains.pluginverifier.client.util.ArchiverUtil
import com.jetbrains.pluginverifier.client.util.BaseCmdUtil
import com.jetbrains.pluginverifier.configurations.CheckPluginParamsParser
import com.jetbrains.pluginverifier.configurations.CheckPluginResults
import com.jetbrains.pluginverifier.output.TeamCityVPrinter
import com.jetbrains.pluginverifier.persistence.GsonHolder
import com.jetbrains.pluginverifier.utils.VOptionsUtil
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.apache.commons.io.FileUtils
import org.jetbrains.plugins.verifier.service.api.TaskId
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

    val archivedPluginFiles: MutableList<Pair<File, Boolean>> = arrayListOf()
    try {
      pluginFiles.mapTo(archivedPluginFiles) {
        if (it.isDirectory) {
          ArchiverUtil.archiveDirectory(it, File.createTempFile("plugin", ".zip")) to true
        } else {
          it to false
        }
      }

      val archivedIdes: MutableList<File> = arrayListOf()
      try {
        ideFiles.mapTo(archivedIdes) {
          ArchiverUtil.archiveDirectory(it, File.createTempFile("ide", ".zip"))
        }

        execute(archivedIdes, archivedPluginFiles.map { it.first }, opts)

      } finally {
        archivedIdes.forEach { FileUtils.deleteQuietly(it) }
      }

    } finally {
      archivedPluginFiles.filter { it.second }.forEach { FileUtils.deleteQuietly(it.first) }
    }

  }

  private fun execute(ideFiles: List<File>, pluginFiles: List<File>, opts: BaseCmdOpts) {
    val service = VerifierService(opts.host)

    val runnerParams = createRunnerParams(opts)

    val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
    builder.addFormDataPart("params", null, RequestBody.create(MediaTypes.JSON, GsonHolder.GSON.toJson(runnerParams)))
    ideFiles.forEachIndexed { id, file -> builder.addFormDataPart("ide_$id", file.name, RequestBody.create(MediaTypes.OCTET_STREAM, file)) }
    pluginFiles.forEachIndexed { id, file -> builder.addFormDataPart("plugin_$id", file.name, RequestBody.create(MediaTypes.OCTET_STREAM, file)) }

    val call = service.enqueueTaskService.checkPlugin(builder.build())
    val response = call.executeSuccessfully()

    val taskId: TaskId
    taskId = parseTaskId(response)
    LOG.info("The task ID is $taskId")
    val results = waitCompletion<CheckPluginResults>(service, taskId)

    processResults(opts, results)
  }

  fun processResults(opts: BaseCmdOpts, results: CheckPluginResults) {
    if (opts.needTeamCityLog) {
      results.printTcLog(TeamCityVPrinter.GroupBy.parse(opts.group), true)
    }
  }


  private fun createRunnerParams(opts: BaseCmdOpts): CheckPluginRunnerParams {
    val jdkVersion: JdkVersion = BaseCmdUtil.parseJdkVersion(opts) ?: throw IllegalArgumentException("Specify the JDK version to check with")
    val vOptions = VOptionsUtil.parseOpts(opts)
    return CheckPluginRunnerParams(jdkVersion, vOptions)
  }

}