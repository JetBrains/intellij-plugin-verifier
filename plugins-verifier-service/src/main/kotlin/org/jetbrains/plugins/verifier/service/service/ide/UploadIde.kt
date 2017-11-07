package org.jetbrains.plugins.verifier.service.service.ide

import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.repository.AvailableIde
import com.jetbrains.pluginverifier.repository.IdeRepository
import org.jetbrains.plugins.verifier.service.service.tasks.BooleanServiceTaskResult
import org.jetbrains.plugins.verifier.service.service.tasks.ServiceTask
import org.jetbrains.plugins.verifier.service.service.tasks.ServiceTaskProgress
import org.jetbrains.plugins.verifier.service.service.tasks.ServiceTaskResult
import org.jetbrains.plugins.verifier.service.storage.IdeFilesManager

/**
 * @author Sergey Patrikeev
 */
class UploadIdeRunner(val availableIde: AvailableIde,
                      val ideRepository: IdeRepository) : ServiceTask() {

  override fun presentableName(): String = "Downloading IDE #$availableIde"

  override fun computeResult(progress: ServiceTaskProgress): ServiceTaskResult {
    val ideFile = ideRepository.getOrDownloadIde(availableIde) {
      progress.setFraction(it)
    }

    try {
      val ideAdded = IdeFilesManager.addIde(ideFile)
      return BooleanServiceTaskResult(ideAdded)
    } finally {
      ideFile.deleteLogged()
    }
  }

}