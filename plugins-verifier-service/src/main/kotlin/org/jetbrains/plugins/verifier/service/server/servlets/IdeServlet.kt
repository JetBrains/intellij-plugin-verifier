package org.jetbrains.plugins.verifier.service.server.servlets

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Servlet responsible for IDE downloading and removing.
 */
class IdeServlet : BaseServlet() {

  override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
    sendJson(resp, serverContext.ideFilesBank.getAvailableIdeVersions())
  }

}
