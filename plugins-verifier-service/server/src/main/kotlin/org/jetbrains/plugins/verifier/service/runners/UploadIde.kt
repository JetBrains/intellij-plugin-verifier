package org.jetbrains.plugins.verifier.service.runners

import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.repository.IdeRepository
import org.jetbrains.plugins.verifier.service.core.Progress
import org.jetbrains.plugins.verifier.service.core.Task
import org.jetbrains.plugins.verifier.service.storage.FileManager
import org.jetbrains.plugins.verifier.service.storage.IdeFilesManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Function

/**
 * @author Sergey Patrikeev
 */
class UploadIdeRunner(val ideVersion: String, val isCommunity: Boolean = false, val fromSnapshots: Boolean = false) : Task<Boolean>() {

  private val LOG: Logger = LoggerFactory.getLogger(UploadIdeRunner::class.java)

  override fun presentableName(): String = "UploadIde"

  override fun computeResult(progress: Progress): Boolean {
    val rawVersion = ideVersion.substringAfter("IU-").substringAfter("IC-")
    val artifact = IdeRepository.fetchIndex(fromSnapshots)
        .find { it.version == rawVersion && it.isCommunity == isCommunity }
        ?: throw IllegalArgumentException("Unable to find the IDE #$ideVersion (community = $isCommunity) in snapshots = $fromSnapshots")

    val ideFile = FileManager.createTempFile(".zip")

    try {
      try {
        IdeRepository.downloadIde(artifact, ideFile, Function<Double, Unit>() { progress.setProgress(it) })
      } catch(e: Exception) {
        LOG.error("Unable to download IDE ${artifact.version} community=${artifact.isCommunity} from snapshots=${artifact.snapshots}", e)
        throw e
      }

      val success = IdeFilesManager.addIde(ideFile)
      LOG.info("IDE #$ideVersion ${if (fromSnapshots) "from snapshots repo" else ""} has been added")
      return success
    } finally {
      ideFile.deleteLogged()
    }
  }

}