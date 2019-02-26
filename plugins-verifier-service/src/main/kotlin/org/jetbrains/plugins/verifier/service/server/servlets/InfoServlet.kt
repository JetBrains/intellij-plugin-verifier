package org.jetbrains.plugins.verifier.service.server.servlets

import com.jetbrains.pluginverifier.parameters.filtering.IgnoreCondition
import org.jetbrains.plugins.verifier.service.server.servlets.info.IgnoredProblemsPage
import org.jetbrains.plugins.verifier.service.server.servlets.info.StatusPage
import org.jetbrains.plugins.verifier.service.service.BaseService
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * The servlet handling requests of the server status, health and parameters.
 */
class InfoServlet : BaseServlet() {

  override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
    val path = getPath(req, resp) ?: return
    when {
      path.endsWith("control-service") -> controlService(req, resp)
      path.endsWith("modify-ignored-problems") -> modifyIgnoredProblems(req, resp)
      path.endsWith("ignored-problems") -> ignoredProblems(resp)
      else -> processStatus(resp)
    }
  }

  private fun sendBadRequest(resp: HttpServletResponse, message: String) {
    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, message)
  }

  private fun modifyIgnoredProblems(req: HttpServletRequest, resp: HttpServletResponse) {
    val ignoredProblems = req.getParameter("ignored.problems")
    val adminPassword = req.getParameter("admin.password")
    if (ignoredProblems == null || adminPassword == null) {
      return sendBadRequest(resp, "Invalid request")
    }
    if (adminPassword != serverContext.authorizationData.serviceAdminPassword) {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Incorrect admin password")
      return
    }
    val ignoreConditions = try {
      parseIgnoreConditions(ignoredProblems)
    } catch (e: Exception) {
      val msg = "Unable to parse ignored problems: ${e.message}"
      logger.warn(msg, e)
      return sendBadRequest(resp, msg)
    }
    serverContext.serviceDAO.replaceIgnoreConditions(ignoreConditions)
    resp.sendRedirect("/info/ignored-problems")
  }

  private fun parseIgnoreConditions(ignoredProblems: String) =
      ignoredProblems.lines()
          .map { it.trim() }
          .filterNot { it.isEmpty() }
          .map { IgnoreCondition.parseCondition(it) }

  private fun ignoredProblems(resp: HttpServletResponse) {
    sendHtml(resp, IgnoredProblemsPage(serverContext.serviceDAO.ignoreConditions).generate())
  }

  private fun controlService(req: HttpServletRequest, resp: HttpServletResponse) {
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
    val service = serverContext.allServices.find { it.serviceName == serviceName }
    if (service == null) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Service $serviceName is not found")
      return
    }
    val command = req.getParameter("command")
    changeServiceState(service, resp, command)
  }

  private fun changeServiceState(
      service: BaseService,
      resp: HttpServletResponse,
      command: String
  ) {
    val success = when (command) {
      "start" -> service.start()
      "resume" -> service.resume()
      "pause" -> service.pause()
      else -> {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Unknown command: $command")
        return
      }
    }

    if (success) {
      resp.sendRedirect("/info/status")
    } else {
      resp.sendError(HttpServletResponse.SC_CONFLICT, "Service's ${service.serviceName} state cannot be changed from ${service.getState()} by command '$command'")
    }
  }

  private fun processStatus(resp: HttpServletResponse) {
    sendHtml(resp, StatusPage(serverContext).generate())
  }

}
