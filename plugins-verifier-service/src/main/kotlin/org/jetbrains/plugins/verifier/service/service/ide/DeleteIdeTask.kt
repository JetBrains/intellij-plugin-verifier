package org.jetbrains.plugins.verifier.service.service.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.IdeFilesBank
import org.jetbrains.plugins.verifier.service.tasks.ProgressIndicator
import org.jetbrains.plugins.verifier.service.tasks.Task

/**
 * [Task] responsible for deleting IDE build having the specified [IDE version] [ideVersion].
 */
class DeleteIdeTask(
    private val ideFilesBank: IdeFilesBank,
    private val ideVersion: IdeVersion
) : Task<Boolean>("DeleteIde #$ideVersion", "DeleteIde") {

  override fun execute(progress: ProgressIndicator) = ideFilesBank.deleteIde(ideVersion)

}