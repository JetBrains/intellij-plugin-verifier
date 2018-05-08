package org.jetbrains.plugins.verifier.service.server.servlets

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.HtmlBuilder
import com.jetbrains.pluginverifier.misc.MemoryInfo
import com.jetbrains.pluginverifier.repository.cleanup.fileSize
import org.jetbrains.plugins.verifier.service.service.BaseService
import org.jetbrains.plugins.verifier.service.service.verifier.ScheduledVerification
import org.jetbrains.plugins.verifier.service.tasks.ServiceTaskState
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * The servlet handling requests of the server status, health and parameters.
 */
class InfoServlet : BaseServlet() {

  companion object {
    private val DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm:ss")
        .withZone(ZoneId.systemDefault())
  }

  override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
    val path = getPath(req, resp) ?: return
    when {
      path.endsWith("control-service") -> processServiceControl(req, resp)
      path.endsWith("unignore-verification") -> processUnignoreVerification(req, resp)
      else -> processStatus(resp)
    }
  }

  private fun processUnignoreVerification(req: HttpServletRequest, resp: HttpServletResponse) {
    val updateId = req.getParameter("updateId")?.toIntOrNull()
    if (updateId == null) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter 'updateId' must be specified")
      return
    }
    val ideVersion = req.getParameter("ideVersion")?.let { IdeVersion.createIdeVersionIfValid(it) }
    if (ideVersion == null) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter 'ideVersion' must be specified")
      return
    }
    val updateInfo = serverContext.pluginRepository.getPluginInfoById(updateId)
    if (updateInfo == null) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Update #$updateId is not found in ${serverContext.pluginRepository}")
      return
    }
    val scheduledVerification = ScheduledVerification(updateInfo, ideVersion)
    serverContext.verificationResultsFilter.unignoreVerificationResultFor(scheduledVerification)
    sendOk(resp, "Verification $scheduledVerification has been unignored")
  }

  private fun processServiceControl(req: HttpServletRequest, resp: HttpServletResponse) {
    val adminPassword = req.getParameter("admin-password")
    if (adminPassword == null || adminPassword != serverContext.authorizationData.serviceAdminPassword) {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Incorrect password")
      return
    }
    val serviceName = req.getParameter("service-name")
    if (serviceName == null) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Service name is not specified")
      return
    }
    val command = req.getParameter("command")
    when (command) {
      "start" -> changeServiceState(serviceName, resp) { it.start() }
      "resume" -> changeServiceState(serviceName, resp) { it.resume() }
      "pause" -> changeServiceState(serviceName, resp) { it.pause() }
      else -> resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Unknown command: $command")
    }
  }

  private fun changeServiceState(serviceName: String, resp: HttpServletResponse, action: (BaseService) -> Boolean) {
    val service = serverContext.allServices.find { it.serviceName == serviceName }
    if (service == null) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Service $serviceName is not found")
    } else {
      if (action(service)) {
        sendOk(resp, "Service's $serviceName state is changed to ${service.getState()}")
      } else {
        resp.sendError(HttpServletResponse.SC_CONFLICT, "Service $serviceName can't be paused")
      }
    }
  }

  private fun processStatus(resp: HttpServletResponse) {
    sendContent(resp, generateStatusPage(), "text/html")
  }

  private fun generateStatusPage(): ByteArray {
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
              +("Plugin Verifier Service $appVersion")
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
            val runningTasks = activeTasks.filter { it.state == ServiceTaskState.RUNNING }
            val waitingTasks = activeTasks.filter { it.state == ServiceTaskState.WAITING }

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
                      td { +String.format("%.2f", progress.fraction) }
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
