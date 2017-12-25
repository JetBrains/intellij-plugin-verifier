package org.jetbrains.plugins.verifier.service.service.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.plugins.verifier.service.server.ServerContext
import org.jetbrains.plugins.verifier.service.tasks.ProgressIndicator
import org.jetbrains.plugins.verifier.service.tasks.ServiceTask

/**
 * Service task responsible for uploading IDE build having the specified [IDE version] [ideVersion]
 * from the [IDE Repository] [com.jetbrains.pluginverifier.ide.IdeRepository].
 */
class UploadIdeTask(val serverContext: ServerContext,
                    private val ideVersion: IdeVersion) : ServiceTask<Boolean>("Downloading IDE $ideVersion") {

  override fun execute(progress: ProgressIndicator) =
      serverContext.ideFilesBank.getIdeFileLock(ideVersion) != null

}