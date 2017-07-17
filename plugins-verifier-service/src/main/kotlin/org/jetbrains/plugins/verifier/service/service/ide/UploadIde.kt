package org.jetbrains.plugins.verifier.service.service.ide

import com.intellij.structure.ide.IdeVersion
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.repository.AvailableIde
import com.jetbrains.pluginverifier.repository.IdeRepository
import org.jetbrains.plugins.verifier.service.ide.IdeFilesManager
import org.jetbrains.plugins.verifier.service.storage.FileManager
import org.jetbrains.plugins.verifier.service.tasks.Task
import org.jetbrains.plugins.verifier.service.tasks.TaskProgress
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Sergey Patrikeev
 */
class UploadIdeRunner(val ideVersion: IdeVersion? = null,
                      val availableIde: AvailableIde? = null,
                      val fromSnapshots: Boolean = false) : Task<Boolean>() {

  init {
    require(ideVersion != null || availableIde != null, { "Ide version to be uploaded is not specified" })
  }

  private val LOG: Logger = LoggerFactory.getLogger(UploadIdeRunner::class.java)

  override fun presentableName(): String = "UploadIde #${availableIde?.version ?: ideVersion!!}"

  override fun computeResult(progress: TaskProgress): Boolean {
    val artifact = getArtifactInfo() ?: throw IllegalArgumentException("Unable to find the IDE #$ideVersion in snapshots = $fromSnapshots")

    val ideFile = FileManager.createTempFile(".zip")

    try {
      try {
        IdeRepository.getOrDownloadIde(artifact) { progress.setFraction(it) }
      } catch(e: Exception) {
        LOG.error("Unable to download IDE ${artifact.version} community=${artifact.isCommunity} from snapshots=${artifact.isSnapshot}", e)
        throw e
      }

      val success = IdeFilesManager.addIde(ideFile)
      LOG.info("IDE #${artifact.version} ${if (fromSnapshots) "from snapshots repo" else ""} has been added")
      return success
    } finally {
      ideFile.deleteLogged()
    }
  }

  private fun getArtifactInfo(): AvailableIde? = availableIde ?: IdeRepository.fetchIndex(fromSnapshots)
      .find { it.version.asStringWithoutProductCode() == ideVersion!!.asStringWithoutProductCode() }

}