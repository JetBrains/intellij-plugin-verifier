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

  override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
    val path = getPath(req, resp) ?: return
    when {
      path.endsWith("uploadIde") -> processUploadIde(req, resp)
      path.endsWith("deleteIde") -> processDeleteIde(req, resp)
      else -> sendJson(resp, serverContext.ideKeeper.getAvailableIdeVersions())
    }
  }

  private fun parseIdeVersionParameter(req: HttpServletRequest, resp: HttpServletResponse): IdeVersion? {
    val ideVersionParam = req.getParameter("ideVersion") ?: return null
    val ideVersion = IdeVersion.createIdeVersionIfValid(ideVersionParam)
    if (ideVersion == null) {
      sendNotFound(resp, "Invalid IDE version: $ideVersionParam")
    }
    return ideVersion
  }

  private fun processUploadIde(req: HttpServletRequest, resp: HttpServletResponse) {
    val parameterIdeVersion = parseIdeVersionParameter(req, resp) ?: return
    val availableIde = serverContext.ideRepository.fetchIndex().find { it.version.asStringWithoutProductCode() == parameterIdeVersion.asStringWithoutProductCode() }
    if (availableIde == null) {
      sendNotFound(resp, "IDE with version $parameterIdeVersion is not found in the ${serverContext.ideRepository}")
      return
    }
    val uploadIdeVersion = availableIde.version
    val ideRunner = UploadIdeTask(serverContext, uploadIdeVersion)
    val taskStatus = serverContext.taskManager.enqueue(ideRunner)
    serverContext.ideKeeper.registerManuallyUploadedIde(uploadIdeVersion)
    sendOk(resp, "Uploading $uploadIdeVersion (#${taskStatus.taskId})")
  }

  private fun processDeleteIde(req: HttpServletRequest, resp: HttpServletResponse) {
    val ideVersion = parseIdeVersionParameter(req, resp) ?: return
    if (serverContext.ideKeeper.isAvailableIde(ideVersion)) {
      val deleteIdeRunner = DeleteIdeTask(serverContext, ideVersion)
      val taskStatus = serverContext.taskManager.enqueue(deleteIdeRunner)
      serverContext.ideKeeper.removeManuallyUploadedIde(ideVersion)
      sendOk(resp, "Deleting $ideVersion (#${taskStatus.taskId})")
    }
  }

}
