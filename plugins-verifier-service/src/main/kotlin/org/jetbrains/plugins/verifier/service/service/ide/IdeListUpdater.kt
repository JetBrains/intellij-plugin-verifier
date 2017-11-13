package org.jetbrains.plugins.verifier.service.service.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.AvailableIde
import org.jetbrains.plugins.verifier.service.server.ServerContext
import org.jetbrains.plugins.verifier.service.service.BaseService
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Patrikeev
 */
class IdeListUpdater(serverContext: ServerContext) : BaseService("IdeListUpdater", 0, 30, TimeUnit.MINUTES, serverContext) {

  private val downloadingIdes: MutableSet<IdeVersion> = hashSetOf()

  override fun doServe() {
    val alreadyIdes = serverContext.ideFilesManager.ideList()

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
    val task = DeleteIdeRunner(ideVersion, serverContext)
    val taskStatus = serverContext.taskManager.enqueue(task)
    LOG.info("Delete IDE #$ideVersion is enqueued with taskId=#${taskStatus.taskId}")
  }

  private fun enqueueUploadIde(availableIde: AvailableIde) {
    val version = availableIde.version
    if (downloadingIdes.contains(version)) {
      return
    }

    val runner = UploadIdeRunner(availableIde, serverContext)

    val taskStatus = serverContext.taskManager.enqueue(
        runner,
        { },
        { _, _ -> }
    ) { _ -> downloadingIdes.remove(version) }
    LOG.info("Uploading IDE version #$version (task #${taskStatus.taskId})")

    downloadingIdes.add(version)
  }

  private fun fetchRelevantIdes(): List<AvailableIde> {
    val index = serverContext.ideRepository.fetchIndex()

    val branchToVersions: Map<Int, List<AvailableIde>> = index
        .filterNot { it.isCommunity }
        .groupBy { it.version.baselineVersion }

    val lastBranchBuilds = branchToVersions.mapValues { it.value.maxBy { it.version } }.values.filterNotNull()
    val lastBranchReleases = branchToVersions.mapValues { it.value.filter { it.isRelease }.maxBy { it.version } }.values.filterNotNull()

    return (lastBranchBuilds + lastBranchReleases)
  }


}