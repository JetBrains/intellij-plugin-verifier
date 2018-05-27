package org.jetbrains.plugins.verifier.service.server.servlets.info

import com.jetbrains.pluginverifier.misc.HtmlBuilder
import com.jetbrains.pluginverifier.misc.MemoryInfo
import com.jetbrains.pluginverifier.repository.cleanup.fileSize
import org.jetbrains.plugins.verifier.service.server.ServerContext
import org.jetbrains.plugins.verifier.service.tasks.TaskDescriptor
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Generates a web page containing service's health and runtime information.
 */
class StatusPage(private val serverContext: ServerContext) {

  companion object {
    private val DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm:ss")
        .withZone(ZoneId.systemDefault())
  }

  fun generateStatusPage(): ByteArray {
    val byteOS = ByteArrayOutputStream()
    val printWriter = PrintWriter(byteOS)
    HtmlBuilder(printWriter).apply {
      html {
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
              +"Runtime parameters:"
            }
            ul {
              serverContext.startupSettings.forEach { s ->
                li {
                  +(s.key + " = " + if (s.encrypted) "*****" else s.get())
                }
              }
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

              val totalUsage = serverContext.applicationHomeDirectory.fileSize
              li { +"Total disk usage: $totalUsage" }
            }

            h2 {
              +"Services:"
            }
            ul {
              serverContext.allServices.forEach { service ->
                val serviceName = service.serviceName
                li {
                  +(serviceName + " - ${service.getState()}")
                  form("control-$serviceName", "display: inline;", "/info/control-service") {
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
              serverContext.ideFilesBank.getAvailableIdeVersions().forEach {
                li {
                  +it.toString()
                }
              }
            }

            val ignoredVerifications = serverContext.verificationResultsFilter.ignoredVerifications
            if (ignoredVerifications.isNotEmpty()) {
              h2 {
                +"Ignored verification results"
              }
              table("width: 100%") {
                tr {
                  th { +"Plugin" }
                  th { +"IDE" }
                  th { +"Time" }
                  th { +"Verdict" }
                  th { +"Reason" }
                }
                ignoredVerifications
                    .forEach { (scheduledVerification, ignore) ->
                      tr {
                        td { +scheduledVerification.updateInfo.toString() }
                        td { +scheduledVerification.ideVersion.toString() }
                        td { +DATE_FORMAT.format(ignore.verificationEndTime) }
                        td { +ignore.verificationVerdict }
                        td { +ignore.ignoreReason }
                      }
                    }
              }
            }

            val activeTasks = serverContext.taskManager.activeTasks
            val runningTasks = activeTasks.filter { it.state == TaskDescriptor.State.RUNNING }
            val waitingTasks = activeTasks.filter { it.state == TaskDescriptor.State.WAITING }

            val finishedTasks = serverContext.taskManager.finishedTasks

            mapOf(
                "Running tasks" to runningTasks.sortedByDescending { it.startTime },
                "Pending tasks (10 latest)" to waitingTasks.sortedByDescending { it.startTime }.take(10),
                "Finished tasks (10 latest)" to finishedTasks.sortedByDescending { it.startTime }.take(10)
            ).forEach { title, tasks ->
              h2 {
                +title
              }
              table("width: 100%") {
                tr {
                  th(style = "width: 5%") { +"ID" }
                  th(style = "width: 20%") { +"Task name" }
                  th(style = "width: 10%") { +"Start time" }
                  th(style = "width: 5%") { +"State" }
                  th(style = "width: 50%") { +"Message" }
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
                      td { +kotlin.String.format("%.2f", progress.fraction) }
                      td { +elapsedTime.toMillis().toString() }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    printWriter.close()
    return byteOS.toByteArray()
  }

}