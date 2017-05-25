package org.jetbrains.plugins.verifier.service.util

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.intellij.structure.ide.IdeVersion
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

  private val downloadingIdes: MutableSet<IdeVersion> = hashSetOf()

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
    try {
      LOG.info("It's time to upload new IDE versions to the verifier service")

      val alreadyIdes: List<IdeVersion> = IdeFilesManager.ideList()

      LOG.info("There are the following IDE on the service now: $alreadyIdes")

      val newList: List<AvailableIde> = fetchNewList()

      LOG.info("The following IDEs should be on the service: ${newList.map { it.version }}")

      val shouldBe: List<Pair<AvailableIde, IdeVersion>> = newList.map { it to fullVersion(it.version) }

      shouldBe.filterNot { alreadyIdes.contains(it.second) }.distinctBy { it.second }.forEach {
        enqueueUploadIde(it.first)
      }

      (alreadyIdes - shouldBe.map { it.second }).forEach {
        enqueueDeleteIde(it)
      }
    } catch (e: Exception) {
      LOG.error("Failed to update IDE list", e)
    }
  }

  private fun fullVersion(ideVersion: IdeVersion): IdeVersion =
      if (ideVersion.productCode.isNullOrEmpty())
        IdeVersion.createIdeVersion("IU-" + ideVersion.asStringWithoutProductCode())
      else ideVersion

  private fun enqueueDeleteIde(ideVersion: IdeVersion) {
    LOG.info("Delete the IDE #$ideVersion because it is not necessary anymore")
    val task = DeleteIdeRunner(ideVersion)
    val taskId = TaskManager.enqueue(task)
    LOG.info("Delete IDE #$ideVersion is enqueued with taskId=#$taskId")
  }

  private fun enqueueUploadIde(availableIde: AvailableIde) {
    val version = availableIde.version
    if (downloadingIdes.contains(version)) {
      LOG.info("The IDE #$version is already being downloaded")
      return
    }

    val runner = UploadIdeRunner(availableIde = availableIde)

    val taskId = TaskManager.enqueue(runner, { }, { _, _, _ -> }, { _, _ -> downloadingIdes.remove(version) })
    LOG.info("Uploading IDE version #$version is enqueued with taskId=#$taskId")

    downloadingIdes.add(version)
  }

  private fun fetchNewList(): List<AvailableIde> {
    val availableIde: List<AvailableIde> = IdeRepository.fetchIndex()

    val branchToVersions: Map<Int, List<AvailableIde>> = availableIde
        .filterNot { it.isCommunity }
        .groupBy { it.version.baselineVersion }

    val lastBranchBuilds = branchToVersions.mapValues { it.value.maxBy { it.version } }.values.filterNotNull()
    val lastBranchReleases = branchToVersions.mapValues { it.value.filter { it.isRelease }.maxBy { it.version } }.values.filterNotNull()

    return (lastBranchBuilds + lastBranchReleases)
  }


}