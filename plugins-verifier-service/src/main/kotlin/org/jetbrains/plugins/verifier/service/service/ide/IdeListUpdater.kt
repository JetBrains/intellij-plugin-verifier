package org.jetbrains.plugins.verifier.service.service.ide

import com.intellij.structure.ide.IdeVersion
import com.jetbrains.pluginverifier.repository.AvailableIde
import com.jetbrains.pluginverifier.repository.IdeRepository
import org.jetbrains.plugins.verifier.service.ide.IdeFilesManager
import org.jetbrains.plugins.verifier.service.service.BaseService
import org.jetbrains.plugins.verifier.service.tasks.TaskManager
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Patrikeev
 */
class IdeListUpdater(taskManager: TaskManager) : BaseService("IdeListUpdater", 0, 30, TimeUnit.MINUTES, taskManager) {

  private val downloadingIdes: MutableSet<IdeVersion> = hashSetOf()

  override fun doTick() {
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
  }

  private fun fullVersion(ideVersion: IdeVersion): IdeVersion =
      if (ideVersion.productCode.isNullOrEmpty())
        IdeVersion.createIdeVersion("IU-" + ideVersion.asStringWithoutProductCode())
      else ideVersion

  private fun enqueueDeleteIde(ideVersion: IdeVersion) {
    LOG.info("Delete the IDE #$ideVersion because it is not necessary anymore")
    val task = DeleteIdeRunner(ideVersion)
    val taskId = taskManager.enqueue(task)
    LOG.info("Delete IDE #$ideVersion is enqueued with taskId=#$taskId")
  }

  private fun enqueueUploadIde(availableIde: AvailableIde) {
    val version = availableIde.version
    if (downloadingIdes.contains(version)) {
      LOG.info("The IDE #$version is already being downloaded")
      return
    }

    val runner = UploadIdeRunner(availableIde = availableIde)

    val taskId = taskManager.enqueue(runner, { }, { _, _, _ -> }, { _, _ -> downloadingIdes.remove(version) })
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