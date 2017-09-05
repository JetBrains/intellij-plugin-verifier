package org.jetbrains.plugins.verifier.service.servlets

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.plugins.verifier.service.ide.IdeFilesManager
import org.jetbrains.plugins.verifier.service.service.ide.DeleteIdeRunner
import org.jetbrains.plugins.verifier.service.service.ide.UploadIdeRunner
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class IdeServlet : BaseServlet() {

  override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
    val path = getPath(req, resp) ?: return
    if (path.endsWith("uploadIde")) {
      processUploadIde(req, resp)
    } else if (path.endsWith("deleteIde")) {
      processDeleteIde(req, resp)
    } else {
      sendJson(resp, IdeFilesManager.ideList())
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
    val ideRunner = UploadIdeRunner(ideVersion)
    val taskStatus = getTaskManager().enqueue(ideRunner)
    sendOk(resp, "Uploading $ideVersion (#${taskStatus.taskId})")
  }

  private fun processDeleteIde(req: HttpServletRequest, resp: HttpServletResponse) {
    val ideVersion = parseIdeVersionParameter(req, resp) ?: return
    val deleteIdeRunner = DeleteIdeRunner(ideVersion)
    val taskStatus = getTaskManager().enqueue(deleteIdeRunner)
    sendOk(resp, "Deleting $ideVersion (#${taskStatus.taskId})")
  }

}
