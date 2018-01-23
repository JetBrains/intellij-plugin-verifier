package org.jetbrains.plugins.verifier.service.service.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.AvailableIde
import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.ide.IdeRepository
import org.jetbrains.plugins.verifier.service.server.ServiceDAO

/**
 * Determines IDE builds which must be kept on the server at the moment.
 * Current implementation selects all the release builds
 * and last builds for each branch.
 *
 * It requests the IDE index from the [IDE Repository] [com.jetbrains.pluginverifier.ide.IdeRepository]
 * and determines which IDEs are missing or no more required by comparing the index result
 * with [IDE files bank] [com.jetbrains.pluginverifier.ide.IdeFilesBank] data.
 */
class IdeKeeper(private val serviceDAO: ServiceDAO,
                private val ideRepository: IdeRepository,
                private val ideFilesBank: IdeFilesBank) {

  fun registerManuallyDownloadedIde(ideVersion: IdeVersion) {
    serviceDAO.manuallyDownloadedIdes.add(ideVersion)
  }

  fun removeManuallyDownloadedIde(ideVersion: IdeVersion) {
    serviceDAO.manuallyDownloadedIdes.remove(ideVersion)
  }

  fun isAvailableIde(ideVersion: IdeVersion): Boolean = ideFilesBank.isAvailable(ideVersion)

  fun getAvailableIdeVersions(): Set<IdeVersion> = ideFilesBank.getAvailableIdeVersions()

  fun getIdeFileLock(ideVersion: IdeVersion) = ideFilesBank.getIdeFileLock(ideVersion)

  fun deleteIde(ideVersion: IdeVersion) = ideFilesBank.deleteIde(ideVersion)

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
    val availableIdes: Set<IdeVersion> = getAvailableIdeVersions()
    val relevantIdes: Set<IdeVersion> = fetchRelevantIdes().map { it.version }.toSet()

    val manuallyDownloadedIdes = serviceDAO.manuallyDownloadedIdes

    val missingIdes: Set<IdeVersion> = relevantIdes - availableIdes
    val unnecessaryIdes: Set<IdeVersion> = availableIdes - relevantIdes.map { it } - manuallyDownloadedIdes

    return IdesList(availableIdes, missingIdes, unnecessaryIdes, manuallyDownloadedIdes)
  }

  /**
   * Fetches IDE index from the [IDE Repository] [com.jetbrains.pluginverifier.ide.IdeRepository]
   * and selects versions which should be kept for the server purposes.
   */
  private fun fetchRelevantIdes(): Set<AvailableIde> {
    val index = ideRepository.fetchIndex()

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