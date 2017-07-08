package org.jetbrains.plugins.verifier.service.servlets

import com.intellij.structure.ide.IdeVersion
import org.jetbrains.plugins.verifier.service.ide.IdeFilesManager
import org.jetbrains.plugins.verifier.service.service.ide.DeleteIdeRunner
import org.jetbrains.plugins.verifier.service.service.ide.UploadIdeRunner
import org.jetbrains.plugins.verifier.service.tasks.TaskManager
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class IdeServlet(taskManager: TaskManager) : BaseServlet(taskManager) {

  override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
    val path = getPath(req, resp) ?: return
    val ideVersionParam = req.getParameter("ideVersion") ?: return
    val ideVersion = try {
      IdeVersion.createIdeVersion(ideVersionParam)
    } catch (e: Exception) {
      sendNotFound(resp, "Invalid IDE version: $ideVersionParam")
      null
    } ?: return

    if (path.endsWith("uploadIde")) {
      processUploadIde(ideVersion, resp)
    } else if (path.endsWith("deleteIde")) {
      processDeleteIde(ideVersion, resp)
    } else {
      sendJson(resp, IdeFilesManager.ideList())
    }
  }

  private fun processUploadIde(ideVersion: IdeVersion, resp: HttpServletResponse) {
    val ideRunner = UploadIdeRunner(ideVersion)
    val taskId = taskManager.enqueue(ideRunner)
    sendOk(resp, "Uploading $ideVersion (#$taskId)")
  }

  private fun processDeleteIde(ideVersion: IdeVersion, resp: HttpServletResponse) {
    val deleteIdeRunner = DeleteIdeRunner(ideVersion)
    val taskId = taskManager.enqueue(deleteIdeRunner)
    sendOk(resp, "Deleting $ideVersion (#$taskId)")
  }

}
