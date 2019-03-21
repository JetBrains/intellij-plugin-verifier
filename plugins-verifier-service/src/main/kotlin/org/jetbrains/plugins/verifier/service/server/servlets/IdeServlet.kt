package org.jetbrains.plugins.verifier.service.server.servlets

import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Servlet responsible for IDE downloading and removing.
 */
@WebServlet(name = "ide", urlPatterns = ["/ide/*"])
class IdeServlet : BaseServlet() {

  override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
    sendJson(resp, serverContext.ideFilesBank.getAvailableIdeVersions())
  }

}
