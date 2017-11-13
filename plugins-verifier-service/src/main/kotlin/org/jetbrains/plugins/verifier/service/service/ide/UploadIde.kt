package org.jetbrains.plugins.verifier.service.service.ide

import com.jetbrains.pluginverifier.ide.AvailableIde
import com.jetbrains.pluginverifier.misc.deleteLogged
import org.jetbrains.plugins.verifier.service.server.ServerContext
import org.jetbrains.plugins.verifier.service.service.tasks.BooleanServiceTaskResult
import org.jetbrains.plugins.verifier.service.service.tasks.ServiceTask
import org.jetbrains.plugins.verifier.service.service.tasks.ServiceTaskProgress
import org.jetbrains.plugins.verifier.service.service.tasks.ServiceTaskResult

/**
 * @author Sergey Patrikeev
 */
class UploadIdeRunner(val availableIde: AvailableIde,
                      serverContext: ServerContext) : ServiceTask(serverContext) {

  override fun presentableName(): String = "Downloading IDE #$availableIde"

  override fun computeResult(progress: ServiceTaskProgress): ServiceTaskResult {
    val ideFile = serverContext.ideRepository.getOrDownloadIde(availableIde) {
      progress.setFraction(it)
    }

    try {
      val ideAdded = serverContext.ideFilesManager.addIde(ideFile, availableIde.version)
      return BooleanServiceTaskResult(ideAdded)
    } finally {
      ideFile.deleteLogged()
    }
  }

}