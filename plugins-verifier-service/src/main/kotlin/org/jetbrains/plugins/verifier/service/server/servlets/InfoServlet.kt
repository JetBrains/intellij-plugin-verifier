package org.jetbrains.plugins.verifier.service.server.servlets

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.plugins.verifier.service.server.servlets.info.StatusPage
import org.jetbrains.plugins.verifier.service.service.BaseService
import org.jetbrains.plugins.verifier.service.service.verifier.ScheduledVerification
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * The servlet handling requests of the server status, health and parameters.
 */
class InfoServlet : BaseServlet() {

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
    sendContent(resp, StatusPage(serverContext).generateStatusPage(), "text/html")
  }

}
