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
    val alreadyIdes: Set<IdeVersion> = serverContext.ideFilesBank.getAvailableIdeVersions()

    val necessaryIdes: Set<IdeVersion> = fetchRelevantIdes().map { it.version }.toSet()

    val missingIdes: Set<IdeVersion> = necessaryIdes - alreadyIdes
    val redundantIdes: Set<IdeVersion> = alreadyIdes - necessaryIdes.map { it }

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

  private fun enqueueUploadIde(ideVersion: IdeVersion) {
    if (downloadingIdes.contains(ideVersion)) {
      return
    }

    val runner = UploadIdeRunner(serverContext, ideVersion)

    val taskStatus = serverContext.taskManager.enqueue(
        runner,
        { },
        { _, _ -> }
    ) { _ -> downloadingIdes.remove(ideVersion) }
    LOG.info("Uploading IDE version #$ideVersion (task #${taskStatus.taskId})")

    downloadingIdes.add(ideVersion)
  }

  private fun fetchRelevantIdes(): Set<AvailableIde> {
    val index = serverContext.ideFilesBank.ideRepository.fetchIndex()

    val branchToVersions: Map<Int, List<AvailableIde>> = index
        .filterNot { it.isCommunity }
        .groupBy { it.version.baselineVersion }

    val lastBranchBuilds = branchToVersions
        .mapValues { it.value.maxBy { it.version } }
        .values.filterNotNull()
    val lastBranchReleases = branchToVersions
        .mapValues { it.value.filter { it.isRelease }.maxBy { it.version } }
        .values.filterNotNull()

    return (lastBranchBuilds + lastBranchReleases).toSet()
  }


}