package org.jetbrains.plugins.verifier.service.service.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.AvailableIde
import org.jetbrains.plugins.verifier.service.server.ServerContext

/**
 * Determinant of IDE builds which must be kept on the server at the moment.
 *
 * It requests the IDE index from the [IDE Repository] [com.jetbrains.pluginverifier.ide.IdeRepository]
 * and determines which IDEs are missing or no more required by comparing the index result
 * with [IDE files bank] [com.jetbrains.pluginverifier.ide.IdeFilesBank] data.
 */
class IdeKeeper(private val serverContext: ServerContext) {

  /**
   * Holds versions of [available] [availableIdes], [missing] [missingIdes] and [redundant] [redundantIdes] IDEs.
   * The _missing_ IDEs are to be uploaded, and the _redundant_ IDEs are to be removed from the server.
   */
  data class IdesList(val availableIdes: Set<IdeVersion>,
                      val missingIdes: Set<IdeVersion>,
                      val redundantIdes: Set<IdeVersion>)

  /**
   * Returns the list of IDEs which must be kept on the server or be deleted because of not being needed.
   */
  fun getIdesList(): IdesList {
    val availableIdes: Set<IdeVersion> = serverContext.ideFilesBank.getAvailableIdeVersions()
    val relevantIdes: Set<IdeVersion> = fetchRelevantIdes().map { it.version }.toSet()

    val missingIdes: Set<IdeVersion> = relevantIdes - availableIdes
    val redundantIdes: Set<IdeVersion> = availableIdes - relevantIdes.map { it }

    return IdesList(availableIdes, missingIdes, redundantIdes)
  }

  /**
   * Fetches the IDE index from the [IDE Repository] [com.jetbrains.pluginverifier.ide.IdeRepository]
   * and selects the versions which should be kept for the server purposes.
   *
   * Current implementation behaves as follows. Of the IDE builds from the release channel
   * it selects all the release versions and last version for every branch.
   */
  private fun fetchRelevantIdes(): Set<AvailableIde> {
    val index = serverContext.ideRepository.fetchIndex()

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