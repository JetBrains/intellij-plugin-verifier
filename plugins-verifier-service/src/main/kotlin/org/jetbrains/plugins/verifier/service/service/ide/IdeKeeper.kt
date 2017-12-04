package org.jetbrains.plugins.verifier.service.service.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.AvailableIde
import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.ide.IdeRepository
import org.jetbrains.plugins.verifier.service.server.ServiceDAO

/**
 * Determinant of IDE builds which must be kept on the server at the moment.
 *
 * It requests the IDE index from the [IDE Repository] [com.jetbrains.pluginverifier.ide.IdeRepository]
 * and determines which IDEs are missing or no more required by comparing the index result
 * with [IDE files bank] [com.jetbrains.pluginverifier.ide.IdeFilesBank] data.
 */
class IdeKeeper(private val serviceDAO: ServiceDAO,
                private val ideRepository: IdeRepository,
                private val ideFilesBank: IdeFilesBank) {

  fun registerManuallyUploadedIde(ideVersion: IdeVersion) {
    serviceDAO.addManuallyUploadedIde(ideVersion)
  }

  fun removeManuallyUploadedIde(ideVersion: IdeVersion) {
    serviceDAO.removeManuallyUploadedIde(ideVersion)
  }

  fun isAvailableIde(ideVersion: IdeVersion): Boolean = ideFilesBank.has(ideVersion)

  fun getAvailableIdeVersions(): Set<IdeVersion> = ideFilesBank.getAvailableIdeVersions()

  /**
   * Holds versions of [available] [availableIdes], [missing] [missingIdes] and [unnecessary] [unnecessaryIdes] IDEs.
   * The _missing_ IDEs are to be uploaded, and the _unnecessary_ IDEs are to be removed from the server.
   * IDE versions specified in [manuallyUploadedIdes] are uploaded manually and should not be affected.
   */
  data class IdesList(val availableIdes: Set<IdeVersion>,
                      val missingIdes: Set<IdeVersion>,
                      val unnecessaryIdes: Set<IdeVersion>,
                      val manuallyUploadedIdes: Set<IdeVersion>)

  /**
   * Returns the list of IDEs which must be kept on the server or be deleted because of not being needed.
   */
  fun getIdesList(): IdesList {
    val availableIdes: Set<IdeVersion> = getAvailableIdeVersions()
    val relevantIdes: Set<IdeVersion> = fetchRelevantIdes().map { it.version }.toSet()

    val manuallyUploadedIdes = serviceDAO.getManuallyUploadedIdes()

    val missingIdes: Set<IdeVersion> = relevantIdes - availableIdes
    val redundantIdes: Set<IdeVersion> = availableIdes - relevantIdes.map { it } - manuallyUploadedIdes

    return IdesList(availableIdes, missingIdes, redundantIdes, manuallyUploadedIdes)
  }

  /**
   * Fetches IDE index from the [IDE Repository] [com.jetbrains.pluginverifier.ide.IdeRepository]
   * and selects versions which should be kept for the server purposes.
   *
   * Current implementation behaves as follows. Of the IDE builds from the release channel
   * it selects all the release versions and last version for every branch.
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