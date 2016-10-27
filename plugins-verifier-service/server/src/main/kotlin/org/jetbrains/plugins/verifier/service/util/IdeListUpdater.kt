package org.jetbrains.plugins.verifier.service.util

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.repository.AvailableIde
import com.jetbrains.pluginverifier.repository.IdeRepository
import org.jetbrains.plugins.verifier.service.core.TaskManager
import org.jetbrains.plugins.verifier.service.runners.DeleteIdeRunner
import org.jetbrains.plugins.verifier.service.runners.UploadIdeRunner
import org.jetbrains.plugins.verifier.service.storage.IdeFilesManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Patrikeev
 */
object IdeListUpdater {

  private val LOG: Logger = LoggerFactory.getLogger(IdeListUpdater::class.java)

  //30 minutes
  private val DOWNLOAD_NEW_IDE_PERIOD: Long = 30

  fun run() {
    Executors.newSingleThreadScheduledExecutor(
        ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("ide-repository-%d")
            .build()
    ).scheduleAtFixedRate({ IdeListUpdater.tick() }, 0, DOWNLOAD_NEW_IDE_PERIOD, TimeUnit.MINUTES)
  }

  @Synchronized
  private fun tick() {
    LOG.info("It's time to upload new IDE versions to the verifier service")

    val alreadyIdes: List<IdeVersion> = IdeFilesManager.ideList()

    LOG.info("There are the following IDE on the service now: $alreadyIdes")

    val newList: List<IdeVersion> = fetchNewList()

    LOG.info("The following IDEs should be on the service: $newList")

    (newList - alreadyIdes).distinct().forEach {
      enqueueUploadIde(it)
    }

    (alreadyIdes - newList).distinct().forEach {
      enqueueDeleteIde(it)
    }
  }

  private fun enqueueDeleteIde(ideVersion: IdeVersion) {
    LOG.info("Delete the IDE #$ideVersion because it is not necessary anymore")
    val task = DeleteIdeRunner(ideVersion)
    val taskId = TaskManager.enqueue(task)
    LOG.info("Delete IDE #$taskId is enqueued with taskId=#$taskId")
  }

  private fun enqueueUploadIde(ideVersion: IdeVersion) {
    val runner = UploadIdeRunner(ideVersion)

    val taskId = TaskManager.enqueue(runner)
    LOG.info("Uploading IDE version #$ideVersion is enqueued with taskId=#$taskId")
  }

  private fun fetchNewList(): List<IdeVersion> {
    val availableIde = IdeRepository.fetchIndex()

    val branchToVersions: Map<Int, List<AvailableIde>> = availableIde
        .filterNot { it.isCommunity }
        .groupBy { it.version.baselineVersion }

    val lastBranchBuilds = branchToVersions.mapValues { it.value.map { it.version }.max() }.values.filterNotNull()
    val lastBranchReleases = branchToVersions.mapValues { it.value.filter { it.isRelease }.map { it.version }.max() }.values.filterNotNull()

    return (lastBranchBuilds + lastBranchReleases)
  }


}