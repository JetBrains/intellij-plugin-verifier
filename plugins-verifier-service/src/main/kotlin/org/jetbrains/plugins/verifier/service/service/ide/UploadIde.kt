package org.jetbrains.plugins.verifier.service.service.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.repository.AvailableIde
import com.jetbrains.pluginverifier.repository.IdeRepository
import org.jetbrains.plugins.verifier.service.ide.IdeFilesManager
import org.jetbrains.plugins.verifier.service.tasks.BooleanServiceTaskResult
import org.jetbrains.plugins.verifier.service.tasks.ServiceTask
import org.jetbrains.plugins.verifier.service.tasks.ServiceTaskProgress
import org.jetbrains.plugins.verifier.service.tasks.ServiceTaskResult

/**
 * @author Sergey Patrikeev
 */
class UploadIdeRunner(val ideVersion: IdeVersion? = null,
                      val availableIde: AvailableIde? = null,
                      val fromSnapshots: Boolean = false,
                      val ideRepository: IdeRepository) : ServiceTask() {

  init {
    require(ideVersion != null || availableIde != null, { "IDE version to be uploaded is not specified" })
  }

  override fun presentableName(): String = "Downloading IDE #${availableIde?.version ?: ideVersion!!}"

  override fun computeResult(progress: ServiceTaskProgress): ServiceTaskResult {
    val artifact = getArtifactInfo() ?: throw IllegalArgumentException("Unable to find the IDE #$ideVersion in snapshots = $fromSnapshots")

    val ideFile = ideRepository.getOrDownloadIde(artifact) { progress.setFraction(it) }

    try {
      val ideAdded = IdeFilesManager.addIde(ideFile)
      return BooleanServiceTaskResult(ideAdded)
    } finally {
      ideFile.deleteLogged()
    }
  }

  private fun getArtifactInfo(): AvailableIde? {
    return availableIde ?: ideRepository.fetchIndex(fromSnapshots)
        .find { it.version.asStringWithoutProductCode() == ideVersion!!.asStringWithoutProductCode() }
  }

}