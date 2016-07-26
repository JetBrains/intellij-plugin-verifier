package com.jetbrains.pluginverifier.client.commands

import com.jetbrains.pluginverifier.client.BaseCmdOpts
import com.jetbrains.pluginverifier.client.network.*
import com.jetbrains.pluginverifier.client.util.ArchiverUtil
import com.jetbrains.pluginverifier.client.util.BaseCmdUtil
import com.jetbrains.pluginverifier.configurations.CheckIdeCompareResult
import com.jetbrains.pluginverifier.output.HtmlVPrinter
import com.jetbrains.pluginverifier.output.TeamCityLog
import com.jetbrains.pluginverifier.output.TeamCityVPrinter
import com.jetbrains.pluginverifier.utils.VOptionsUtil
import org.jetbrains.plugins.verifier.service.api.TaskId
import org.jetbrains.plugins.verifier.service.params.CheckTrunkApiRunnerParams
import org.jetbrains.plugins.verifier.service.results.CheckTrunkApiResults
import org.jetbrains.plugins.verifier.service.util.deleteLogged
import org.slf4j.LoggerFactory
import java.io.File

/**
 * @author Sergey Patrikeev
 */
class CheckTrunkApiCommand : Command {
  override fun name(): String = "check-trunk-api"

  companion object {
    private val LOG = LoggerFactory.getLogger(CheckTrunkApiCommand::class.java)
  }

  override fun execute(opts: BaseCmdOpts, freeArgs: List<String>) {
    if (freeArgs.isEmpty()) {
      throw IllegalArgumentException("You have to specify the IDE to check")
    }
    var ideFile = File(freeArgs[0])
    if (!ideFile.exists()) {
      throw IllegalArgumentException("IDE $ideFile doesn't exist")
    }

    val runnerParams = checkTrunkApiRunnerParams(opts)
    LOG.debug("The runner params: $runnerParams")

    val paramsPart = MultipartUtil.createJsonPart("params", runnerParams)
    var delete: Boolean = false
    if (ideFile.isDirectory) {
      val tempFile = File.createTempFile("ide", ".zip")
      try {
        ideFile = ArchiverUtil.archiveDirectory(ideFile, tempFile)
      } catch (e: Exception) {
        tempFile.deleteLogged()
        throw RuntimeException("Unable to pack the file $ideFile")
      }
      delete = true
    }

    val service = VerifierService(opts.host)
    val taskId: TaskId
    try {
      val filePart = MultipartUtil.createFilePart("ideFile", ideFile)
      val call = service.enqueueTaskService.checkTrunkApi(filePart, paramsPart)
      taskId = parseTaskId(call.executeSuccessfully())
      LOG.debug("The task ID is $taskId")
    } finally {
      if (delete) {
        ideFile.deleteLogged()
      }
    }

    val checkTrunkApiResults = waitCompletion<CheckTrunkApiResults>(service, taskId)
    processResults(checkTrunkApiResults, opts)
  }

  private fun processResults(apiResults: CheckTrunkApiResults, opts: BaseCmdOpts) {
    if (opts.needTeamCityLog) {
      val vPrinter = TeamCityVPrinter(TeamCityLog(System.out), TeamCityVPrinter.GroupBy.parse(opts))
      val compareResult = CheckIdeCompareResult.compareWithPreviousReports(listOf(apiResults.majorReport), apiResults.currentReport)
      vPrinter.printIdeCompareResult(compareResult)
    }
    if (opts.saveCheckIdeReport != null) {
      val file = File(opts.saveCheckIdeReport)
      apiResults.currentReport.saveToFile(file)
    }
    if (opts.htmlReportFile != null) {
      HtmlVPrinter(apiResults.currentReport.ideVersion, { false }, File(opts.htmlReportFile))
    }
  }

  private fun checkTrunkApiRunnerParams(opts: BaseCmdOpts): CheckTrunkApiRunnerParams {
    val jdkVersion = BaseCmdUtil.parseJdkVersion(opts) ?: throw IllegalArgumentException("Specify the JDK version to check with")
    val vOptions = VOptionsUtil.parseOpts(opts)

    return CheckTrunkApiRunnerParams(jdkVersion, vOptions)
  }

}