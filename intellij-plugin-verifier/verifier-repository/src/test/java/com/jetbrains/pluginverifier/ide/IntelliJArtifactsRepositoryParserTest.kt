package com.jetbrains.pluginverifier.ide

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.jetbrains.plugin.structure.ide.IntelliJPlatformProduct
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.repositories.ArtifactsJson
import com.jetbrains.pluginverifier.ide.repositories.IntelliJIdeRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.net.URL
import java.time.LocalDate

class IntelliJArtifactsRepositoryParserTest {

  companion object {
    private const val REPO_URL = "https://cache-redirector.jetbrains.com/intellij-repository/releases"
  }

  /**
   * Ensures that .json index of the IDE repository is parsed properly:
   * `https://www.jetbrains.com/intellij-repository/snapshots/index.json`
   */
  @Test
  fun `simple index parsing test`() {
    val artifactsJson = Gson().fromJson<ArtifactsJson>(
      IntelliJArtifactsRepositoryParserTest::class.java.getResourceAsStream("/intelliJArtifactsRepositoryIndex.json").bufferedReader()
    )

    val artifacts = IntelliJRepositoryIndexParser().parseArtifacts(artifactsJson.artifacts, IntelliJIdeRepository.Channel.RELEASE)

    val expectedArtifacts = listOf(
      AvailableIde(
        IdeVersion.createIdeVersion("IU-181.3870.7"),
        null,
        URL("$REPO_URL/com/jetbrains/intellij/idea/ideaIU/181.3870.7/ideaIU-181.3870.7.zip"),
        LocalDate.of(2018, 2, 20),
        IntelliJPlatformProduct.IDEA
      ),

      AvailableIde(
        IdeVersion.createIdeVersion("IC-181.3870.7"),
        null,
        URL("$REPO_URL/com/jetbrains/intellij/idea/ideaIC/181.3870.7/ideaIC-181.3870.7.zip"),
        LocalDate.of(2018, 2, 20),
        IntelliJPlatformProduct.IDEA_IC
      ),

      AvailableIde(
        IdeVersion.createIdeVersion("IU-173.4548.28"),
        "2017.3.4",
        URL("$REPO_URL/com/jetbrains/intellij/idea/ideaIU/2017.3.4/ideaIU-2017.3.4.zip"),
        LocalDate.of(2018, 1, 30),
        IntelliJPlatformProduct.IDEA
      ),

      AvailableIde(
        IdeVersion.createIdeVersion("IC-173.4548.28"),
        "2017.3.4",
        URL("$REPO_URL/com/jetbrains/intellij/idea/ideaIC/2017.3.4/ideaIC-2017.3.4.zip"),
        LocalDate.of(2018, 1, 30),
        IntelliJPlatformProduct.IDEA_IC
      ),

      AvailableIde(
        IdeVersion.createIdeVersion("RD-173.3994.2442"),
        null,
        URL("$REPO_URL/com/jetbrains/intellij/rider/riderRD/173.3994.2442/riderRD-173.3994.2442.zip"),
        LocalDate.of(2018, 2, 6),
        IntelliJPlatformProduct.RIDER
      ),

      AvailableIde(
        IdeVersion.createIdeVersion("MPS-181.1168"),
        "2018.1",
        URL("$REPO_URL/com/jetbrains/mps/mps/2018.1/mps-2018.1.zip"),
        LocalDate.of(2018, 4, 6),
        IntelliJPlatformProduct.MPS
      )
    )

    assertEquals(expectedArtifacts.size, artifacts.size)
    for (expectedArtifact in expectedArtifacts) {
      val sameArtifact = artifacts.find { it.version == expectedArtifact.version }
      assertNotNull("$expectedArtifact is not found", sameArtifact)
      assertEquals("$expectedArtifact", expectedArtifact.downloadUrl, sameArtifact!!.downloadUrl)
      assertEquals("$expectedArtifact", expectedArtifact.releaseVersion, sameArtifact.releaseVersion)
      assertEquals("$expectedArtifact", expectedArtifact.uploadDate, sameArtifact.uploadDate)
    }
  }

}