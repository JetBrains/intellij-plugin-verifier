package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.repositories.ArtifactJson
import com.jetbrains.pluginverifier.ide.repositories.IntelliJIdeRepository
import com.jetbrains.pluginverifier.ide.repositories.setProductCodeIfAbsent
import java.net.URL

/**
 * Utility class used to parse index of available IDEs from [IntelliJIdeRepository].
 */
internal class IntelliJRepositoryIndexParser {

  fun parseArtifacts(artifacts: List<ArtifactJson>, channel: IntelliJIdeRepository.Channel): List<AvailableIde> {
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

      val ideArtifacts = artifactsOfVersion.filter {
        it.packaging == "zip" && IntelliJIdeRepository.getProductCodeByArtifactId(it.artifactId) != null
      }

      for (artifactInfo in ideArtifacts) {
        val ideVersion = IdeVersion.createIdeVersionIfValid(buildNumber)
            ?.setProductCodeIfAbsent(IntelliJIdeRepository.getProductCodeByArtifactId(artifactInfo.artifactId)!!)
            ?: continue

        val downloadUrl = buildDownloadUrl(artifactInfo, channel, groupId, version)

        val isRelease = channel == IntelliJIdeRepository.Channel.RELEASE && isReleaseLikeVersion(artifactInfo.version)
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

  private fun buildDownloadUrl(
      artifactInfo: ArtifactJson,
      channel: IntelliJIdeRepository.Channel,
      groupId: String,
      version: String
  ) = with(artifactInfo) {
    URL(channel.repositoryUrl + "/${groupId.replace('.', '/')}/$artifactId/$version/$artifactId-$version.$packaging")
  }

  /**
   * From [ides], which is a list of IDEs with the same versions,
   * selects the most appropriate.
   *
   * For one [AvailableIde.version] there might be two [AvailableIde]s:
   * one with `isRelease = true` and another with `isRelease = false`.
   * We'd like to keep only the release one.
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