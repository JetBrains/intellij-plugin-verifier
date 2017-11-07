package org.jetbrains.plugins.verifier.service.server.servlets

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.plugins.verifier.service.server.ServerInstance
import org.jetbrains.plugins.verifier.service.service.ide.DeleteIdeRunner
import org.jetbrains.plugins.verifier.service.service.ide.UploadIdeRunner
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class IdeServlet : BaseServlet() {

  //todo: protect IDEs which are explicitly uploaded by this method from removing by the IDE cleaner
  override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
    val path = getPath(req, resp) ?: return
    when {
      path.endsWith("uploadIde") -> processUploadIde(req, resp)
      path.endsWith("deleteIde") -> processDeleteIde(req, resp)
      else -> sendJson(resp, ServerInstance.ideFilesManager.ideList())
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
    val availableIde = ServerInstance.ideRepository.fetchIndex().find { it.version.asStringWithoutProductCode() == ideVersion.asStringWithoutProductCode() }
    if (availableIde == null) {
      sendNotFound(resp, "IDE with version $ideVersion is not found in the ${ServerInstance.ideRepository}")
      return
    }
    val ideRunner = UploadIdeRunner(availableIde, ServerInstance.ideRepository)
    val taskStatus = ServerInstance.taskManager.enqueue(ideRunner)
    sendOk(resp, "Uploading $ideVersion (#${taskStatus.taskId})")
  }

  private fun processDeleteIde(req: HttpServletRequest, resp: HttpServletResponse) {
    val ideVersion = parseIdeVersionParameter(req, resp) ?: return
    val deleteIdeRunner = DeleteIdeRunner(ideVersion)
    val taskStatus = ServerInstance.taskManager.enqueue(deleteIdeRunner)
    sendOk(resp, "Deleting $ideVersion (#${taskStatus.taskId})")
  }

}
