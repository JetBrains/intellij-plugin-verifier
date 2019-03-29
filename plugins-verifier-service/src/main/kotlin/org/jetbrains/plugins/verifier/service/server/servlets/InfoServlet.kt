package org.jetbrains.plugins.verifier.service.server.servlets

import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.pluginverifier.parameters.filtering.IgnoreCondition
import org.jetbrains.plugins.verifier.service.server.servlets.info.IgnoredProblemsPage
import org.jetbrains.plugins.verifier.service.server.servlets.info.StatusPage
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * The servlet handling requests of the server status, health and parameters.
 */
@WebServlet(name = "info", urlPatterns = ["/info/*"])
class InfoServlet : BaseServlet() {

  override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
    val path = getPath(req, resp) ?: return
    when {
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
      e.rethrowIfInterrupted()
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

  private fun processStatus(resp: HttpServletResponse) {
    sendHtml(resp, StatusPage(serverContext).generate())
  }

}
