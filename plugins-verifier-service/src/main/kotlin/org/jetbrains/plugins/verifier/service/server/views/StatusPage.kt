/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.plugins.verifier.service.server.views

import com.jetbrains.plugin.structure.base.utils.pluralizeWithNumber
import com.jetbrains.pluginverifier.misc.HtmlBuilder
import com.jetbrains.pluginverifier.misc.MemoryInfo
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import org.jetbrains.plugins.verifier.service.server.ServerContext
import org.jetbrains.plugins.verifier.service.tasks.TaskDescriptor
import org.jetbrains.plugins.verifier.service.tasks.TaskManager
import org.springframework.web.servlet.View
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Generates a web page containing service's health and runtime information.
 */
class StatusPage(
  private val serverContext: ServerContext,
  private val taskManager: TaskManager
) : View {
  companion object {
    private val DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm:ss")
      .withZone(ZoneId.systemDefault())
  }

  override fun getContentType() = "text/html"

  override fun render(model: MutableMap<String, *>?, request: HttpServletRequest, response: HttpServletResponse) {
    response.outputStream.buildHtml {
      head {
        title("Server status")
        style {
          +"""table, th, td {
            border: 1px solid black;
            border-collapse: collapse;
          }"""
        }
      }
      body {
        div {
          h1 {
            +("Plugin Verifier Service ${serverContext.appVersion}")
          }

          h2 {
            +"Status:"
          }
          ul {
            val (totalMemory, freeMemory, usedMemory, maxMemory) = MemoryInfo.getRuntimeMemoryInfo()
            li { +"Total memory: $totalMemory" }
            li { +"Free memory: $freeMemory" }
            li { +"Used memory: $usedMemory" }
            li { +"Max memory: $maxMemory" }

            val totalIdeFilesSize = serverContext.ideFilesBank.getAvailableIdeFiles()
              .map { it.fileInfo.fileSize }
              .fold(SpaceAmount.ZERO_SPACE) { acc, v -> acc + v }

            val totalPluginsSize = serverContext.pluginFilesBank.getAvailablePluginFiles()
              .map { it.fileInfo.fileSize }
              .fold(SpaceAmount.ZERO_SPACE) { acc, v -> acc + v }

            li { +"IDEs disk usage: $totalIdeFilesSize" }
            li { +"Plugins disk usage: $totalPluginsSize" }
          }

          h2 {
            +"Services:"
          }
          ul {
            serverContext.allServices.forEach { service ->
              val serviceName = service.serviceName
              li {
                +(serviceName + " - ${service.getState()}")
                form("control-$serviceName", "display: inline;", "/control-service", method = "post") {
                  input("submit", "command", "start")
                  input("submit", "command", "resume")
                  input("submit", "command", "pause")
                  input("hidden", "service-name", serviceName)
                  +"Admin password: "
                  input("password", "admin-password")
                }
              }
            }
          }

          h2 {
            +"Available IDEs: "
          }
          ul {
            serverContext.ideFilesBank.getAvailableIdeVersions().sorted().forEach {
              li {
                +it.toString()
              }
            }
          }

          val failedAttempts = serverContext.verificationResultsFilter.failedAttempts
          if (failedAttempts.isNotEmpty()) {
            h2 {
              +"Failed verifications"
            }
            table("width: 100%") {
              tr {
                th(style = "width: 30%") { +"Plugin" }
                th(style = "width: 70%") { +"Reason" }
              }
              for ((updateInfo, attempts) in failedAttempts) {
                tr {
                  td { +updateInfo.toString() }
                  td {
                    pre {
                      +buildString {
                        appendLine("We have tried to verify $updateInfo " + "time".pluralizeWithNumber(attempts.size))
                        for (attempt in attempts.sortedByDescending { it.verificationEndTime }) {
                          appendLine("    ${attempt.verificationResult.verificationTarget} on ${DATE_FORMAT.format(attempt.verificationEndTime)}")
                          appendLine("        ${attempt.failureReason.reason}")
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          val activeTasks = taskManager.activeTasks
          val lastFinishedTasks = taskManager.lastFinishedTasks

          buildTaskTable("Finished tasks (20 latest)", lastFinishedTasks.sortedByDescending { it.endTime }.take(20))

          /**
           * Task types that must have even empty tables displayed.
           */
          val nonEmptyTablesTaskTypes = listOf("VerifyPlugin", "ExtractFeatures", "SendIdes")
          val allTaskTypes = activeTasks.keys + nonEmptyTablesTaskTypes

          for (taskType in allTaskTypes) {
            val tasks = activeTasks[taskType].orEmpty()
            buildTaskTable("Running $taskType tasks", tasks.filter { it.state == TaskDescriptor.State.RUNNING })
            buildTaskTable("Waiting $taskType tasks (${tasks.size} total) (20 latest shown)", tasks.filter { it.state == TaskDescriptor.State.WAITING }.take(20))
          }
        }
      }
    }
  }

  private fun HtmlBuilder.buildTaskTable(title: String, tasks: List<TaskDescriptor>) {
    h2 {
      +title
    }
    table("width: 100%") {
      tr {
        th(style = "width: 5%") { +"ID" }
        th(style = "width: 30%") { +"Task name" }
        th(style = "width: 10%") { +"Start time" }
        th(style = "width: 5%") { +"State" }
        th(style = "width: 40%") { +"Message" }
        th(style = "width: 5%") { +"Completion %" }
        th(style = "width: 5%") { +"Total time (ms)" }
      }

      tasks.forEach {
        with(it) {
          tr {
            td { +taskId.toString() }
            td { +presentableName }
            td { +DATE_FORMAT.format(startTime) }
            td { +state.toString() }
            td { +progress.text }
            td { +String.format("%.2f", progress.fraction * 100) }
            td { +elapsedTime.toMillis().toString() }
          }
        }
      }
    }
  }

}