package org.jetbrains.plugins.verifier.service.server.servlets.info

import com.jetbrains.pluginverifier.misc.MemoryInfo
import com.jetbrains.pluginverifier.plugin.PluginFilesBank
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import org.jetbrains.plugins.verifier.service.server.ServerContext
import org.jetbrains.plugins.verifier.service.tasks.TaskDescriptor
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

  fun generate() = buildHtml {
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

          val totalIdeFilesSize = serverContext.ideFilesBank.getAvailableIdeFiles()
              .map { it.fileInfo.fileSize }
              .fold(SpaceAmount.ZERO_SPACE) { acc, v -> acc + v }

          val totalPluginsSize = (serverContext.pluginDetailsCache.pluginFileProvider as PluginFilesBank).getAvailablePluginFiles()
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
              form("control-$serviceName", "display: inline;", "/info/control-service", method = "post") {
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

        val ignoredVerifications = serverContext.verificationResultsFilter.ignoredVerifications
        if (ignoredVerifications.isNotEmpty()) {
          h2 {
            +"Ignored verification results"
          }
          table("width: 100%") {
            tr {
              th(style = "width: 20%") { +"Plugin" }
              th(style = "width: 10%") { +"IDE" }
              th(style = "width: 10%") { +"Time" }
              th(style = "width: 30%") { +"Verdict" }
              th(style = "width: 30%") { +"Reason" }
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

        val lastFinishedTasks = serverContext.taskManager.lastFinishedTasks

        mapOf(
            "Running tasks (${runningTasks.size} total)" to runningTasks.sortedByDescending { it.startTime },
            "Pending tasks (${waitingTasks.size} total) (20 latest)" to waitingTasks.sortedByDescending { it.startTime }.take(20),
            "Finished tasks (20 latest)" to lastFinishedTasks.sortedByDescending { it.startTime }.take(20)
        ).forEach { title, tasks ->
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