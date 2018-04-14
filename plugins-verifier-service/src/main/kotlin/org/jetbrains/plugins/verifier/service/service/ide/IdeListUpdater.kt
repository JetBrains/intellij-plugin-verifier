package org.jetbrains.plugins.verifier.service.service.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.AvailableIde
import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.ide.IdeRepository
import org.jetbrains.plugins.verifier.service.server.ServiceDAO
import org.jetbrains.plugins.verifier.service.service.BaseService
import org.jetbrains.plugins.verifier.service.tasks.ServiceTaskManager
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Service responsible for maintaining a set of relevant IDE versions
 * on the server. Being run periodically, it determines a list of IDE builds
 * that should be kept by fetching the IDE index from the IDE Repository ([ImportantIdesDeterminer]).
 */
class IdeListUpdater(taskManager: ServiceTaskManager,
                     serviceDAO: ServiceDAO,
                     ideRepository: IdeRepository,
                     private val ideFilesBank: IdeFilesBank)
  : BaseService("IdeListUpdater", 0, 30, TimeUnit.MINUTES, taskManager) {

  private val ideBuildsTracker = ImportantIdesDeterminer(serviceDAO, ideRepository, ideFilesBank)

  private val downloadingIdes = Collections.synchronizedSet(hashSetOf<IdeVersion>())

  override fun doServe() {
    val (availableIdes, missingIdes, unnecessaryIdes, manuallyDownloadedIdes) = ideBuildsTracker.getIdesList()
    logger.info("""Available IDEs: $availableIdes;
      Missing IDEs: $missingIdes;
      Unnecessary IDEs: $unnecessaryIdes;
      Manually downloaded IDEs: $manuallyDownloadedIdes""")

    missingIdes.forEach {
      enqueueDownloadIde(it)
    }

    unnecessaryIdes.forEach {
      enqueueDeleteIde(it)
    }
  }

  private fun enqueueDeleteIde(ideVersion: IdeVersion) {
    if (ideFilesBank.isLockedOrBeingProvided(ideVersion)) {
      return
    }
    logger.info("Delete IDE #$ideVersion because it is not necessary anymore")
    val task = DeleteIdeTask(ideFilesBank, ideVersion)
    val taskStatus = taskManager.enqueue(task)
    logger.info("Delete IDE #$ideVersion is enqueued with taskId=#${taskStatus.taskId}")
  }

  private fun enqueueDownloadIde(ideVersion: IdeVersion) {
    if (downloadingIdes.contains(ideVersion)) {
      return
    }

    val runner = DownloadIdeTask(ideFilesBank, ideVersion)

    val taskStatus = taskManager.enqueue(
        runner,
        { _, _ -> },
        { _, _ -> },
        { _, _ -> },
        { _ -> downloadingIdes.remove(ideVersion) }
    )
    logger.info("Downloading IDE version #$ideVersion (task #${taskStatus.taskId})")

    downloadingIdes.add(ideVersion)
  }


}

/**
 * Determines IDE builds which must be kept on the server at the moment.
 * Current implementation selects all the release builds
 * and last builds for each branch.
 *
 * It requests the IDE index from the [IDE Repository] [com.jetbrains.pluginverifier.ide.IdeRepository]
 * and determines which IDEs are missing or no more required by comparing the index result with [ideFilesBank] data.
 */
private class ImportantIdesDeterminer(private val serviceDAO: ServiceDAO,
                                      private val ideRepository: IdeRepository,
                                      private val ideFilesBank: IdeFilesBank) {

  /**
   * Holds versions of [available] [availableIdes], [missing] [missingIdes] and [unnecessary] [unnecessaryIdes] IDEs.
   * The _missing_ IDEs are to be downloaded, and the _unnecessary_ IDEs are to be removed from the server.
   * IDE versions specified in [manuallyDownloadedIdes] are downloaded manually and should not be affected.
   */
  data class IdesList(val availableIdes: Set<IdeVersion>,
                      val missingIdes: Set<IdeVersion>,
                      val unnecessaryIdes: Set<IdeVersion>,
                      val manuallyDownloadedIdes: Set<IdeVersion>)

  /**
   * Returns the list of IDEs which must be kept on the server or be deleted because of not being needed.
   */
  fun getIdesList(): IdesList {
    val availableIdes: Set<IdeVersion> = ideFilesBank.getAvailableIdeVersions()
    val relevantIdes: Set<IdeVersion> = fetchRelevantIdes().map { it.version }.toSet()

    val manuallyDownloadedIdes = serviceDAO.manuallyDownloadedIdes

    val missingIdes: Set<IdeVersion> = relevantIdes - availableIdes
    val unnecessaryIdes: Set<IdeVersion> = availableIdes - relevantIdes - manuallyDownloadedIdes

    return IdesList(availableIdes, missingIdes, unnecessaryIdes, manuallyDownloadedIdes)
  }

  /**
   * Fetches IDE index from the [IDE Repository] [com.jetbrains.pluginverifier.ide.IdeRepository]
   * and selects versions which should be kept for the server purposes.
   */
  private fun fetchRelevantIdes(): Set<AvailableIde> {
    val index = ideRepository.fetchIndex()

    val branchToVersions: Map<Int, List<AvailableIde>> = index
        .filter { it.version.productCode == "IU" }
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