package org.jetbrains.plugins.verifier.service.service.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.IdeFilesBank
import org.jetbrains.plugins.verifier.service.service.BaseService
import org.jetbrains.plugins.verifier.service.tasks.ServiceTaskManager
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Service responsible for maintaining a set of relevant IDE versions
 * on the server. Being run periodically, it determines a list of IDE builds
 * that should be kept by fetching the IDE index from the IDE Repository ([IdeKeeper]).
 */
class IdeListUpdater(taskManager: ServiceTaskManager,
                     private val ideKeeper: IdeKeeper,
                     private val ideFilesBank: IdeFilesBank)
  : BaseService("IdeListUpdater", 0, 30, TimeUnit.MINUTES, taskManager) {

  private val downloadingIdes = Collections.synchronizedSet(hashSetOf<IdeVersion>())

  override fun doServe() {
    val (availableIdes, missingIdes, unnecessaryIdes, manuallyUploadedIdes) = ideKeeper.getIdesList()
    logger.info("""Available IDEs: $availableIdes;
      Missing IDEs: $missingIdes;
      Unnecessary IDEs: $unnecessaryIdes;
      Manually uploaded IDEs: $manuallyUploadedIdes""")

    missingIdes.forEach {
      enqueueUploadIde(it)
    }

    unnecessaryIdes.forEach {
      enqueueDeleteIde(it)
    }
  }

  private fun enqueueDeleteIde(ideVersion: IdeVersion) {
    logger.info("Delete IDE #$ideVersion because it is not necessary anymore")
    val task = DeleteIdeTask(ideFilesBank, ideVersion)
    val taskStatus = taskManager.enqueue(task)
    logger.info("Delete IDE #$ideVersion is enqueued with taskId=#${taskStatus.taskId}")
  }

  private fun enqueueUploadIde(ideVersion: IdeVersion) {
    if (downloadingIdes.contains(ideVersion)) {
      return
    }

    val runner = UploadIdeTask(ideFilesBank, ideVersion)

    val taskStatus = taskManager.enqueue(
        runner,
        { _, _ -> },
        { _, _ -> }
    ) { _ -> downloadingIdes.remove(ideVersion) }
    logger.info("Uploading IDE version #$ideVersion (task #${taskStatus.taskId})")

    downloadingIdes.add(ideVersion)
  }


}