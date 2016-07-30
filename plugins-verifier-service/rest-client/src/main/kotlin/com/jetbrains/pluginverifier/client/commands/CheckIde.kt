package com.jetbrains.pluginverifier.client.commands

import com.jetbrains.pluginverifier.client.BaseCmdOpts
import com.jetbrains.pluginverifier.client.network.*
import com.jetbrains.pluginverifier.client.util.ArchiverUtil
import com.jetbrains.pluginverifier.client.util.BaseCmdUtil
import com.jetbrains.pluginverifier.configurations.CheckIdeParamsParser
import com.jetbrains.pluginverifier.configurations.CheckIdeResults
import com.jetbrains.pluginverifier.output.TeamCityVPrinter
import com.jetbrains.pluginverifier.report.CheckIdeReport
import com.jetbrains.pluginverifier.utils.CmdUtil
import com.jetbrains.pluginverifier.utils.VOptionsUtil
import org.jetbrains.plugins.verifier.service.api.TaskId
import org.jetbrains.plugins.verifier.service.params.CheckIdeRunnerParams
import org.jetbrains.plugins.verifier.service.params.JdkVersion
import org.jetbrains.plugins.verifier.service.util.deleteLogged
import org.slf4j.LoggerFactory
import java.io.File

/**
 * @author Sergey Patrikeev
 */
class CheckIdeCommand : Command {
  override fun name(): String = "check-ide"

  companion object {
    private val LOG = LoggerFactory.getLogger(CheckIdeCommand::class.java)
  }

  override fun execute(opts: BaseCmdOpts, freeArgs: List<String>) {
    if (freeArgs.isEmpty()) {
      throw IllegalArgumentException("You should specify the IDE to check")
    }
    var ideFile = File(freeArgs[0])
    if (!ideFile.exists()) {
      throw IllegalArgumentException("IDE $ideFile doesn't exist")
    }

    val runnerParams = checkIdeRunnerParams(opts)
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

    LOG.info("Enqueue the check-ide task of $ideFile")
    val service = VerifierService(opts.host)

    val taskId: TaskId
    try {
      val filePart = MultipartUtil.createFilePart("ideFile", ideFile)

      val call = service.enqueueTaskService.checkIde(filePart, paramsPart)
      taskId = parseTaskId(call.executeSuccessfully())
      LOG.info("The task ID is $taskId")

    } finally {
      if (delete) {
        ideFile.deleteLogged()
      }
    }

    val checkIdeResults = waitCompletion<CheckIdeResults>(service, taskId)

    processResults(checkIdeResults, opts, service)
  }

  fun processResults(checkIdeResults: CheckIdeResults, opts: BaseCmdOpts, service: VerifierService) {
    val report = CheckIdeReport.createReport(checkIdeResults.ideVersion, checkIdeResults.vResults)

    if (opts.saveCheckIdeReport != null) {
      val file = File(opts.saveCheckIdeReport)
      report.saveToFile(file)
      uploadReport(file, service)
    }
    if (opts.needTeamCityLog) {
      checkIdeResults.printTcLog(TeamCityVPrinter.GroupBy.parse(opts.group), true)
    }
    if (opts.htmlReportFile != null) {
      checkIdeResults.saveToHtmlFile(File(opts.htmlReportFile))
    }
    if (opts.dumpBrokenPluginsFile != null) {
      checkIdeResults.dumbBrokenPluginsList(File(opts.dumpBrokenPluginsFile))
    }
  }

  fun uploadReport(file: File, service: VerifierService) {
    val filePart = MultipartUtil.createFilePart("reportFile", file)
    service.reportsService.uploadReport(filePart).executeSuccessfully()
  }


  private fun checkIdeRunnerParams(opts: BaseCmdOpts): CheckIdeRunnerParams {
    val jdkVersion: JdkVersion = BaseCmdUtil.parseJdkVersion(opts) ?: throw IllegalArgumentException("Specify the JDK version to check with")

    val actualIdeVersion = CmdUtil.takeVersionFromCmd(opts)
    val vOptions = VOptionsUtil.parseOpts(opts)

    if (opts.externalClasspath.isNotEmpty()) TODO() //TODO: external jars

    val (checkAllBuilds, checkLastBuilds) = CheckIdeParamsParser.parsePluginToCheckList(opts)
    val excludedPlugins = CheckIdeParamsParser.parseExcludedPlugins(opts)

    return CheckIdeRunnerParams(jdkVersion, vOptions, checkAllBuilds, checkLastBuilds, excludedPlugins, actualIdeVersion)
  }


}