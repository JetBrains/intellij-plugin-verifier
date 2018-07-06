package org.jetbrains.plugins.verifier.service.service.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.IdeFilesBank
import org.jetbrains.plugins.verifier.service.tasks.ProgressIndicator
import org.jetbrains.plugins.verifier.service.tasks.Task

/**
 * [Task] responsible for downloading IDE build having the specified [IDE version] [ideVersion]
 * from the [IDE Repository] [com.jetbrains.pluginverifier.ide.IdeRepository].
 */
class DownloadIdeTask(
    private val ideFilesBank: IdeFilesBank,
    private val ideVersion: IdeVersion
) : Task<Boolean>("Downloading IDE $ideVersion", "Download IDE") {

  override fun execute(progress: ProgressIndicator): Boolean {
    //initiates downloading of the IDE.
    val result = ideFilesBank.getIdeFile(ideVersion)
    (result as? IdeFilesBank.Result.Found)?.ideFileLock?.release()
    return result is IdeFilesBank.Result.Found
  }

}