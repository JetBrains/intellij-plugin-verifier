package org.jetbrains.plugins.verifier.service.service.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.IdeFilesBank
import org.jetbrains.plugins.verifier.service.tasks.ProgressIndicator
import org.jetbrains.plugins.verifier.service.tasks.ServiceTask

/**
 * Service task responsible for deleting IDE build having the specified [IDE version] [ideVersion].
 */
class DeleteIdeTask(val ideFilesBank: IdeFilesBank,
                    private val ideVersion: IdeVersion) : ServiceTask<Boolean>("DeleteIde #$ideVersion") {

  override fun execute(progress: ProgressIndicator): Boolean {
    ideFilesBank.deleteIde(ideVersion)
    return true
  }

}