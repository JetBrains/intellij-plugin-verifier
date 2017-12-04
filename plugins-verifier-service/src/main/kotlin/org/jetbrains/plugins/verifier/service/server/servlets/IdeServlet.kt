package org.jetbrains.plugins.verifier.service.server.servlets

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.plugins.verifier.service.service.ide.DeleteIdeTask
import org.jetbrains.plugins.verifier.service.service.ide.UploadIdeTask
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Servlet responsible for IDE uploading and removing.
 */
class IdeServlet : BaseServlet() {

  //todo: protect IDEs which are explicitly uploaded by this method from removing by the IDE cleaner
  override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
    val path = getPath(req, resp) ?: return
    when {
      path.endsWith("uploadIde") -> processUploadIde(req, resp)
      path.endsWith("deleteIde") -> processDeleteIde(req, resp)
      else -> sendJson(resp, serverContext.ideFilesBank.getAvailableIdeVersions())
    }
  }

  private fun parseIdeVersionParameter(req: HttpServletRequest, resp: HttpServletResponse): IdeVersion? {
    val ideVersionParam = req.getParameter("ideVersion") ?: return null
    return try {
      IdeVersion.createIdeVersion(ideVersionParam)
    } catch (e: Exception) {
      sendNotFound(resp, "Invalid IDE version: $ideVersionParam")
      null
    }
  }

  private fun processUploadIde(req: HttpServletRequest, resp: HttpServletResponse) {
    val ideVersion = parseIdeVersionParameter(req, resp) ?: return
    val availableIde = serverContext.ideRepository.fetchIndex().find { it.version.asStringWithoutProductCode() == ideVersion.asStringWithoutProductCode() }
    if (availableIde == null) {
      sendNotFound(resp, "IDE with version $ideVersion is not found in the ${serverContext.ideRepository}")
      return
    }
    val ideRunner = UploadIdeTask(serverContext, availableIde.version)
    val taskStatus = serverContext.taskManager.enqueue(ideRunner)
    sendOk(resp, "Uploading $ideVersion (#${taskStatus.taskId})")
  }

  private fun processDeleteIde(req: HttpServletRequest, resp: HttpServletResponse) {
    val ideVersion = parseIdeVersionParameter(req, resp) ?: return
    val deleteIdeRunner = DeleteIdeTask(serverContext, ideVersion)
    val taskStatus = serverContext.taskManager.enqueue(deleteIdeRunner)
    sendOk(resp, "Deleting $ideVersion (#${taskStatus.taskId})")
  }

}
