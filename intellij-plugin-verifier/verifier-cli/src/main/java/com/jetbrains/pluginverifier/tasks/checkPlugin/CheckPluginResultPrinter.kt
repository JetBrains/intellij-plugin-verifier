/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.output.*
import com.jetbrains.pluginverifier.output.html.HtmlResultPrinter
import com.jetbrains.pluginverifier.output.markdown.MarkdownResultPrinter
import com.jetbrains.pluginverifier.output.stream.WriterResultPrinter
import com.jetbrains.pluginverifier.output.teamcity.TeamCityHistory
import com.jetbrains.pluginverifier.output.teamcity.TeamCityLog
import com.jetbrains.pluginverifier.output.teamcity.TeamCityResultPrinter
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.TaskResult
import com.jetbrains.pluginverifier.tasks.TaskResultPrinter
import java.io.PrintWriter

class CheckPluginResultPrinter(private val pluginRepository: PluginRepository) : TaskResultPrinter {

  override fun printResults(taskResult: TaskResult, outputOptions: OutputOptions) {
    with(taskResult as CheckPluginResult) {
      if (outputOptions.teamCityLog != null) {
        val teamCityHistory = printTcLog(true, outputOptions.teamCityLog, outputOptions.teamCityGroupType)
        outputOptions.postProcessTeamCityTests(teamCityHistory)
      } else {
        if (outputOptions.usePlainOutput()) {
          printOnStdout (this)
        }
      }

      results.groupBy { it.verificationTarget }.forEach { (verificationTarget, resultsOfIde) ->
        if (outputOptions.useHtml()) {
          HtmlResultPrinter(verificationTarget, outputOptions).printResults(resultsOfIde)
        }
        if (outputOptions.useMarkdown()) {
          MarkdownResultPrinter.create(verificationTarget, outputOptions).use {
            it.printResults(resultsOfIde)
          }
        }
      }
    }
  }

  private fun CheckPluginResult.printTcLog(setBuildStatus: Boolean, tcLog: TeamCityLog, groupBy: TeamCityResultPrinter.GroupBy): TeamCityHistory {
    val teamCityHistory = TeamCityResultPrinter(
      tcLog,
      groupBy,
      pluginRepository
    ).printResults(results)

    TeamCityResultPrinter.printInvalidPluginFiles(tcLog, invalidPluginFiles)

    if (setBuildStatus) {
      setTeamCityBuildStatus(tcLog)
    }
    return teamCityHistory
  }

  private fun CheckPluginResult.setTeamCityBuildStatus(tcLog: TeamCityLog) {
    val totalProblemsNumber = results.flatMap {
      when (it) {
        is PluginVerificationResult.InvalidPlugin -> setOf(Any())
        is PluginVerificationResult.Verified -> it.compatibilityProblems
        else -> emptySet()
      }
    }.distinct().size
    if (totalProblemsNumber > 0) {
      tcLog.buildStatusFailure("$totalProblemsNumber problem${if (totalProblemsNumber > 1) "s" else ""} found")
    }
  }

  private fun printOnStdout(checkPluginResult: CheckPluginResult) {
    with(checkPluginResult) {
      val printWriter = PrintWriter(System.out)
      val writerResultPrinter = WriterResultPrinter(printWriter)
      writerResultPrinter.printResults(results)
      writerResultPrinter.printInvalidPluginFiles(invalidPluginFiles)
      printWriter.flush()
    }
  }

}