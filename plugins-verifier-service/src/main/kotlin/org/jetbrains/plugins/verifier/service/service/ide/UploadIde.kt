package org.jetbrains.plugins.verifier.service.service.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.plugins.verifier.service.server.ServerContext
import org.jetbrains.plugins.verifier.service.service.tasks.BooleanServiceTaskResult
import org.jetbrains.plugins.verifier.service.service.tasks.ServiceTask
import org.jetbrains.plugins.verifier.service.service.tasks.ServiceTaskProgress
import org.jetbrains.plugins.verifier.service.service.tasks.ServiceTaskResult

/**
 * @author Sergey Patrikeev
 */
class UploadIdeRunner(serverContext: ServerContext,
                      val ideVersion: IdeVersion) : ServiceTask(serverContext) {

  override fun presentableName(): String = "Downloading IDE #$ideVersion"

  override fun computeResult(progress: ServiceTaskProgress): ServiceTaskResult {
    serverContext.ideFilesBank.get(ideVersion)
    return BooleanServiceTaskResult(true)
  }

}