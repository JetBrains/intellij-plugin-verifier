package org.jetbrains.plugins.verifier.service.service.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.plugins.verifier.service.tasks.ProgressIndicator
import org.jetbrains.plugins.verifier.service.tasks.ServiceTask

/**
 * [ServiceTask] responsible for deleting IDE build having the specified [IDE version] [ideVersion].
 */
class DeleteIdeTask(private val ideKeeper: IdeKeeper,
                    private val ideVersion: IdeVersion) : ServiceTask<Boolean>("DeleteIde #$ideVersion") {

  override fun execute(progress: ProgressIndicator) = ideKeeper.deleteIde(ideVersion)

}