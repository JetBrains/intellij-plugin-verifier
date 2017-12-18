package org.jetbrains.plugins.verifier.service.service.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.plugins.verifier.service.server.ServerContext
import org.jetbrains.plugins.verifier.service.tasks.ProgressIndicator
import org.jetbrains.plugins.verifier.service.tasks.ServiceTask

/**
 * Service task responsible for deleting IDE build having the specified [IDE version] [ideVersion].
 */
class DeleteIdeTask(val serverContext: ServerContext,
                    private val ideVersion: IdeVersion) : ServiceTask<Boolean>("DeleteIde #$ideVersion") {

  override fun execute(progress: ProgressIndicator): Boolean {
    serverContext.ideFilesBank.deleteIde(ideVersion)
    return true
  }

}