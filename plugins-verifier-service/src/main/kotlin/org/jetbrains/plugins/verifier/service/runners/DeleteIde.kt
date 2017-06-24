package org.jetbrains.plugins.verifier.service.runners

import com.intellij.structure.ide.IdeVersion
import org.jetbrains.plugins.verifier.service.core.Progress
import org.jetbrains.plugins.verifier.service.core.Task
import org.jetbrains.plugins.verifier.service.storage.IdeFilesManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Sergey Patrikeev
 */
class DeleteIdeRunner(val ideVersion: IdeVersion) : Task<Boolean>() {

  private val LOG: Logger = LoggerFactory.getLogger(DeleteIdeRunner::class.java)

  override fun presentableName(): String = "DeleteIde #$ideVersion"

  override fun computeResult(progress: Progress): Boolean {
    IdeFilesManager.deleteIde(ideVersion)
    LOG.info("Delete IDE #$ideVersion task is enqueued for IdeFilesManager")
    return true
  }

}