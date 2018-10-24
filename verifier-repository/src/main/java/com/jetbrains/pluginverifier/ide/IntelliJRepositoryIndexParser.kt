package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.net.URL

/**
 * Utility class used to parse
 * [releases](https://www.jetbrains.com/intellij-repository/releases)
 * and
 * [snapshots](https://www.jetbrains.com/intellij-repository/snapshots/)
 * repositories to get lists of [AvailableIde]s.
 */
internal class IntelliJRepositoryIndexParser {

  companion object {

    /**
     * Maps known artifact IDs to IDE product codes.
     *
     * If a new IDE is published to the /snapshots repository,
     * it should be registered here.
     */
    private val artifactIdToIdeProductCode = mapOf(
        "ideaIC" to "IC",
        "ideaIU" to "IU",
        "riderRD" to "RD",
        "mps" to "MPS"
    )
  }

  fun parseArtifacts(artifacts: List<ArtifactJson>, snapshots: Boolean): List<AvailableIde> {
    val allAvailableIdes = arrayListOf<AvailableIde>()

    /**
     * (group-id, version) is a unique key for grouping artifacts.
     */
    val groupedArtifacts = artifacts.groupBy { it.groupId to it.version }
    for ((groupAndVersion, artifactsOfVersion) in groupedArtifacts) {
      val (groupId, version) = groupAndVersion

      /**
       * Build number of this IDE is written in "BUILD".content artifact's key.
       */
      val buildNumber = artifactsOfVersion.find { it.artifactId == "BUILD" }?.content ?: continue

      /**
       * Find IDE builds artifacts.
       * They have "zip" packaging and "artifactId" one of [artifactIdToIdeProductCode].
       */
      val ideArtifacts = artifactsOfVersion.filter { it.packaging == "zip" && it.artifactId in artifactIdToIdeProductCode }

      for (artifactInfo in ideArtifacts) {
        val ideVersion = IdeVersion.createIdeVersionIfValid(buildNumber)
            ?.setProductCodeIfAbsent(artifactIdToIdeProductCode.getValue(artifactInfo.artifactId))
            ?: continue

        val downloadUrl = buildDownloadUrl(artifactInfo, snapshots, groupId, version)

        val isRelease = !snapshots && isReleaseLikeVersion(artifactInfo.version)
        val releasedVersion = version.takeIf { isRelease }
        val availableIde = AvailableIde(ideVersion, releasedVersion, downloadUrl)
        allAvailableIdes.add(availableIde)
      }
    }

    /**
     * Remove duplicated IDEs.
     */
    return allAvailableIdes
        .groupBy { it.version }
        .mapValues { getUniqueIde(it.value) }
        .values.toList()
  }

  /**
   * An example of the download URL is
   * https://www.jetbrains.com/intellij-repository/snapshots/com/jetbrains/intellij/idea/ideaIU/182-SNAPSHOT/ideaIU-182-SNAPSHOT.zip
   */
  private fun buildDownloadUrl(artifactInfo: ArtifactJson, snapshots: Boolean, groupId: String, version: String): URL {
    return URL(
        with(artifactInfo) {
          IntelliJIdeRepository.INTELLIJ_REPOSITORY_URL +
              (if (snapshots) "snapshots" else "releases") +
              "/${groupId.replace('.', '/')}/$artifactId/$version/$artifactId-$version.$packaging"
        }
    )
  }

  /**
   * From [ides], which is a list of IDEs with the same versions,
   * selects the most appropriate.
   *
   * For one [AvailableIde.version] there might be two [AvailableIde]s:
   * one with `isRelease = true` and another with `isRelease = false`.
   * We'd like to keep only the release one.
   * For snapshots channel we want to keep only one IDE.
   */
  private fun getUniqueIde(ides: List<AvailableIde>): AvailableIde =
      if (ides.size == 2) {
        val first = ides.first()
        val second = ides.last()
        if (first.isRelease != second.isRelease) {
          if (first.isRelease) {
            first
          } else {
            second
          }
        } else {
          first
        }
      } else {
        ides.first()
      }

  /**
   * Examples of release versions:
   * - 2017.3
   * - 2016.2.4
   * - 15.0.2
   *
   * Note that IDEs prior to 2016 were numbered as ``13.*, 14.*, 15.*``
   * and IDEs as of 2016 are numbered as ``2016.*``, ``2017.*``, ...
   */
  private fun isReleaseLikeVersion(version: String): Boolean {
    val ideVersion = IdeVersion.createIdeVersionIfValid(version)
    return ideVersion != null && (ideVersion.baselineVersion <= 15 || ideVersion.baselineVersion >= 2016)
  }

}