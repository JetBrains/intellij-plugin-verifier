package org.jetbrains.plugins.verifier.service.service.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.plugins.verifier.service.tasks.ProgressIndicator
import org.jetbrains.plugins.verifier.service.tasks.ServiceTask

/**
 * [ServiceTask] responsible for downloading IDE build having the specified [IDE version] [ideVersion]
 * from the [IDE Repository] [com.jetbrains.pluginverifier.ide.IdeRepository].
 */
class DownloadIdeTask(private val ideKeeper: IdeKeeper,
                      private val ideVersion: IdeVersion) : ServiceTask<Boolean>("Downloading IDE $ideVersion") {

  override fun execute(progress: ProgressIndicator): Boolean {
    //initiates downloading of the IDE.
    val fileLock = ideKeeper.getIdeFileLock(ideVersion)
    fileLock?.release()
    return fileLock != null
  }

}