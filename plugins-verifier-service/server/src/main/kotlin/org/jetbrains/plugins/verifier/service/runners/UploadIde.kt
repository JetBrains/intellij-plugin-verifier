package org.jetbrains.plugins.verifier.service.runners

import org.jetbrains.plugins.verifier.service.core.Progress
import org.jetbrains.plugins.verifier.service.core.Task
import org.jetbrains.plugins.verifier.service.storage.IdeFilesManager
import org.jetbrains.plugins.verifier.service.util.IdeRepository
import org.jetbrains.plugins.verifier.service.util.deleteLogged
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Sergey Patrikeev
 */
class UploadIdeRunner(val ideVersion: String, val isCommunity: Boolean = false, val fromSnapshots: Boolean = false) : Task<Boolean>() {

  private val LOG: Logger = LoggerFactory.getLogger(UploadIdeRunner::class.java)

  override fun presentableName(): String = "UploadIde"

  override fun computeResult(progress: Progress): Boolean {
    val ideFile = IdeRepository.download(ideVersion, progress, isCommunity, fromSnapshots)
    try {
      val success = IdeFilesManager.addIde(ideFile)
      LOG.info("IDE #$ideVersion ${if (fromSnapshots) "from snapshots repo" else ""} has been added")
      return success
    } finally {
      ideFile.deleteLogged()
    }
  }

}