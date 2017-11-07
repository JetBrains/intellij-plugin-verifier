package org.jetbrains.plugins.verifier.service.service.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.plugins.verifier.service.service.tasks.BooleanServiceTaskResult
import org.jetbrains.plugins.verifier.service.service.tasks.ServiceTask
import org.jetbrains.plugins.verifier.service.service.tasks.ServiceTaskProgress
import org.jetbrains.plugins.verifier.service.service.tasks.ServiceTaskResult
import org.jetbrains.plugins.verifier.service.storage.IdeFilesManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Sergey Patrikeev
 */
class DeleteIdeRunner(val ideVersion: IdeVersion) : ServiceTask() {

  private val LOG: Logger = LoggerFactory.getLogger(DeleteIdeRunner::class.java)

  override fun presentableName(): String = "DeleteIde #$ideVersion"

  override fun computeResult(progress: ServiceTaskProgress): ServiceTaskResult {
    IdeFilesManager.deleteIde(ideVersion)
    LOG.info("Delete IDE #$ideVersion task is enqueued for IdeFilesManager")
    return BooleanServiceTaskResult(true)
  }

}