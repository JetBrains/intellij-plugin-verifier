package org.jetbrains.plugins.verifier.service.service.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.plugins.verifier.service.server.ServerContext
import org.jetbrains.plugins.verifier.service.service.tasks.BooleanServiceTaskResult
import org.jetbrains.plugins.verifier.service.service.tasks.ServiceTask
import org.jetbrains.plugins.verifier.service.service.tasks.ServiceTaskProgress
import org.jetbrains.plugins.verifier.service.service.tasks.ServiceTaskResult

/**
 * Service task responsible for uploading IDE build having the specified [IDE version] [ideVersion]
 * from the [IDE Repository] [com.jetbrains.pluginverifier.ide.IdeRepository].
 */
class UploadIdeTask(serverContext: ServerContext,
                    private val ideVersion: IdeVersion) : ServiceTask(serverContext) {

  override fun presentableName(): String = "Downloading IDE $ideVersion"

  override fun computeResult(progress: ServiceTaskProgress): ServiceTaskResult {
    val success = serverContext.ideFilesBank.getIdeLock(ideVersion) != null
    return BooleanServiceTaskResult(success)
  }

}