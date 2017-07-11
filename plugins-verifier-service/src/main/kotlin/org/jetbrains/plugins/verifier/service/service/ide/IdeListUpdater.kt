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
    val alreadyIdes = IdeFilesManager.ideList()

    val relevantIdes: List<AvailableIde> = fetchRelevantIdes()

    val missingIdes: List<AvailableIde> = relevantIdes.filterNot { it.version in alreadyIdes }
    val redundantIdes: List<IdeVersion> = alreadyIdes - relevantIdes.map { it.version }

    LOG.info("Available IDEs: $alreadyIdes;\nMissing IDEs: $missingIdes;\nRedundant IDEs: $redundantIdes")

    missingIdes.forEach {
      enqueueUploadIde(it)
    }

    redundantIdes.forEach {
      enqueueDeleteIde(it)
    }
  }

  private fun enqueueDeleteIde(ideVersion: IdeVersion) {
    LOG.info("Delete the IDE #$ideVersion because it is not necessary anymore")
    val task = DeleteIdeRunner(ideVersion)
    val taskId = taskManager.enqueue(task)
    LOG.info("Delete IDE #$ideVersion is enqueued with taskId=#$taskId")
  }

  private fun enqueueUploadIde(availableIde: AvailableIde) {
    val version = availableIde.version
    if (downloadingIdes.contains(version)) {
      return
    }

    val runner = UploadIdeRunner(availableIde = availableIde)

    val taskId = taskManager.enqueue(runner, { }, { _, _, _ -> }) { _, _ -> downloadingIdes.remove(version) }
    LOG.info("Uploading IDE version #$version (task #$taskId)")

    downloadingIdes.add(version)
  }

  private fun fetchRelevantIdes(): List<AvailableIde> {
    val index = IdeRepository.fetchIndex()

    val branchToVersions: Map<Int, List<AvailableIde>> = index
        .filterNot { it.isCommunity }
        .groupBy { it.version.baselineVersion }

    val lastBranchBuilds = branchToVersions.mapValues { it.value.maxBy { it.version } }.values.filterNotNull()
    val lastBranchReleases = branchToVersions.mapValues { it.value.filter { it.isRelease }.maxBy { it.version } }.values.filterNotNull()

    return (lastBranchBuilds + lastBranchReleases)
  }


}