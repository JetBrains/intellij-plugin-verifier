package org.jetbrains.plugins.verifier.service.servlets

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class InfoServlet : BaseServlet() {

  override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
    val path = getPath(req, resp) ?: return
    if (path.endsWith("status")) {
      processStatus(req, resp)
    }
  }

  private fun processStatus(req: HttpServletRequest, resp: HttpServletResponse) {
    val statusPage = generateStatusPage()
    sendBytes(resp, statusPage.toByteArray(), "text/html")
  }

  private fun generateStatusPage(): String {
    //todo:
    return "TODO"
  }

}
