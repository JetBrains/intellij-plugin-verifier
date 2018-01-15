package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL

/**
 * Utility class that requests metadata from the [IDE repository](https://www.jetbrains.com/intellij-repository/releases)
 * on [available] [AvailableIde] IDEs.
 */
class IdeRepository(val repositoryUrl: String) {

  private val snapshotsRepoUrl = repositoryUrl.trimEnd('/') + "/intellij-repository/snapshots/"

  private val releasesRepoUrl = repositoryUrl.trimEnd('/') + "/intellij-repository/releases/"

  companion object {
    fun setProductCodeIfAbsent(ideVersion: IdeVersion, productCode: String): IdeVersion = if (ideVersion.productCode.isEmpty())
      IdeVersion.createIdeVersion("$productCode-" + ideVersion.asStringWithoutProductCode())
    else {
      ideVersion
    }
  }

  /**
   * Parse the repository's HTML page:
   * https://www.jetbrains.com/intellij-repository/releases/
   */
  private fun parseRepositoryAvailableIdes(document: Document, snapshots: Boolean): List<AvailableIde> {
    val table = document.getElementsByTag("table")[0]
    val tbody = table.getElementsByTag("tbody")[0]
    val tableRows = tbody.getElementsByTag("tr")

    val result = arrayListOf<AvailableIde>()

    tableRows.forEach {
      val columns = it.getElementsByTag("td")

      val version = columns[0].text().trim()
      val buildNumberString = columns[2].text().trim()
      val buildNumber = IdeVersion.createIdeVersion(buildNumberString)
      val isRelease = version != buildNumberString

      val artifacts = columns[3]
      val ideaIU = artifacts.getElementsContainingOwnText("ideaIU.zip")
      if (ideaIU.isNotEmpty()) {
        val downloadIdeaIU = URL(ideaIU[0].attr("href"))
        val fullVersion = setProductCodeIfAbsent(buildNumber, "IU")
        result.add(AvailableIde(fullVersion, isRelease, false, snapshots, downloadIdeaIU))
      }

      val ideaIC = artifacts.getElementsContainingOwnText("ideaIC.zip")
      if (ideaIC.isNotEmpty()) {
        val downloadIdeaIC = URL(ideaIC[0].attr("href"))
        val fullVersion = setProductCodeIfAbsent(buildNumber, "IC")
        result.add(AvailableIde(fullVersion, isRelease, true, snapshots, downloadIdeaIC))
      }
    }

    return result
  }


  private fun getRepositoryUrl(snapshots: Boolean) = if (snapshots) snapshotsRepoUrl else releasesRepoUrl

  fun fetchIndex(snapshots: Boolean = false): List<AvailableIde> {
    val repoUrl = getRepositoryUrl(snapshots)
    val document = Jsoup
        .connect(repoUrl)
        .timeout(3000)
        .get()
    return parseRepositoryAvailableIdes(document, snapshots)
  }

  fun fetchAvailableIdeDescriptor(ideVersion: IdeVersion, snapshots: Boolean = false): AvailableIde? {
    val fullIdeVersion = setProductCodeIfAbsent(ideVersion, "IU")
    return fetchIndex(snapshots).find { it.version == fullIdeVersion }
  }

  override fun toString(): String = "IDE Repository on $repositoryUrl"

}
