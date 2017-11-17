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
class DeleteIdeRunner(val ideVersion: IdeVersion, serverContext: ServerContext) : ServiceTask(serverContext) {

  override fun presentableName(): String = "DeleteIde #$ideVersion"

  override fun computeResult(progress: ServiceTaskProgress): ServiceTaskResult {
    serverContext.ideFilesBank.deleteIde(ideVersion)
    return BooleanServiceTaskResult(true)
  }

}