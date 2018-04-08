package com.jetbrains.pluginverifier.ide

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URL

class IdeRepositoryTest {
  /**
   * Ensures that .json index of the IDE repository is parsed properly:
   * `https://www.jetbrains.com/intellij-repository/releases/index.json`
   */
  @Test
  fun `simple index parsing test`() {
    val artifactsJson = Gson().fromJson<ArtifactsJson>(
        IdeRepositoryTest::class.java.getResourceAsStream("/ideRepositoryIndex.json").bufferedReader()
    )

    val repositoryUrl = "https://www.jetbrains.com/intellij-repository/"
    val indexParser = IdeRepositoryIndexParser(repositoryUrl)
    val artifacts = indexParser.parseArtifacts(artifactsJson.artifacts, false)

    val expectedArtifacts = listOf(
        AvailableIde(
            IdeVersion.createIdeVersion("IU-181.3870.7"),
            null,
            false,
            URL("https://www.jetbrains.com/intellij-repository/releases/com/jetbrains/intellij/idea/ideaIU/181.3870.7/ideaIU-181.3870.7.zip")
        ),

        AvailableIde(
            IdeVersion.createIdeVersion("IC-181.3870.7"),
            null,
            false,
            URL("https://www.jetbrains.com/intellij-repository/releases/com/jetbrains/intellij/idea/ideaIC/181.3870.7/ideaIC-181.3870.7.zip")
        ),

        AvailableIde(
            IdeVersion.createIdeVersion("IU-173.4548.28"),
            "2017.3.4",
            false,
            URL("https://www.jetbrains.com/intellij-repository/releases/com/jetbrains/intellij/idea/ideaIU/2017.3.4/ideaIU-2017.3.4.zip")
        ),

        AvailableIde(
            IdeVersion.createIdeVersion("IC-173.4548.28"),
            "2017.3.4",
            false,
            URL("https://www.jetbrains.com/intellij-repository/releases/com/jetbrains/intellij/idea/ideaIC/2017.3.4/ideaIC-2017.3.4.zip")
        ),

        AvailableIde(
            IdeVersion.createIdeVersion("RD-173.3994.2442"),
            null,
            false,
            URL("https://www.jetbrains.com/intellij-repository/releases/com/jetbrains/intellij/rider/riderRD/173.3994.2442/riderRD-173.3994.2442.zip")
        ),

        AvailableIde(
            IdeVersion.createIdeVersion("MPS-181.1168"),
            "2018.1",
            false,
            URL("https://www.jetbrains.com/intellij-repository/releases/com/jetbrains/mps/mps/2018.1/mps-2018.1.zip")
        )
    )

    assertEquals(expectedArtifacts.size, artifacts.size)
    for (expectedArtifact in expectedArtifacts) {
      assertTrue("$expectedArtifact must be parsed", expectedArtifact in artifacts)
    }
  }

}