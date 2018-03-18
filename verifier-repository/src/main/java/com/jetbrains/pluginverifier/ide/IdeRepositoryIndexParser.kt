package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.net.URL

internal class IdeRepositoryIndexParser(private val repositoryUrl: String) {

  companion object {
    private val artifactIdToIdeProductCode = mapOf("ideaIC" to "IC", "ideaIU" to "IU", "riderRD" to "RD")
  }

  fun parseArtifacts(artifacts: List<ArtifactJson>, snapshots: Boolean): List<AvailableIde> {
    val allAvailableIdes = arrayListOf<AvailableIde>()
    for ((groupAndVersion, withVersion) in artifacts.groupBy { it.groupId to it.version }) {
      val (groupId, version) = groupAndVersion
      val actualBuildNumber = withVersion.find { it.artifactId == "BUILD" }?.content ?: continue
      val ideArtifacts = withVersion.filter { it.packaging == "zip" && it.artifactId in artifactIdToIdeProductCode }
      for (artifactInfo in ideArtifacts) {
        val ideVersion = IdeVersion.createIdeVersionIfValid(actualBuildNumber)
            ?.setProductCodeIfAbsent(artifactIdToIdeProductCode.getValue(artifactInfo.artifactId))
            ?: continue

        /**
         * An example of the download URL is
         * https://www.jetbrains.com/intellij-repository/snapshots/com/jetbrains/intellij/idea/ideaIU/182-SNAPSHOT/ideaIU-182-SNAPSHOT.zip
         */
        val downloadUrl = with(artifactInfo) {
          "${repositoryUrl.trimEnd('/')}/" +
              (if (snapshots) "snapshots" else "releases") +
              "/${groupId.replace('.', '/')}/$artifactId/$version/$artifactId-$version.$packaging"
        }.let { URL(it) }

        val isRelease = !snapshots && isReleaseLikeVersion(artifactInfo.version)
        val releasedVersion = version.takeIf { isRelease }
        val availableIde = AvailableIde(ideVersion, releasedVersion, snapshots, downloadUrl)
        allAvailableIdes.add(availableIde)
      }
    }

    return allAvailableIdes.groupBy { it.version }.mapValues {
      with(it.value) {
        getUniqueIde()
      }
    }.values.toList()
  }

  /**
   * For one [AvailableIde.version] there might be two [AvailableIde]s:
   * one with `isRelease = true` and another with `isRelease = false`.
   * We'd like to keep only the release one.
   * For snapshots channel we want to keep only one IDE.
   */
  private fun List<AvailableIde>.getUniqueIde(): AvailableIde =
      if (size == 2) {
        val first = first()
        val second = last()
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
        first()
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