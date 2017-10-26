package com.jetbrains.pluginverifier.results

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.local.LocalPluginInfo
import com.jetbrains.pluginverifier.repository.local.meta.LocalRepositoryMetadataParser
import org.junit.Assert
import org.junit.Test
import java.io.File

/**
 * Created by Sergey.Patrikeev
 */
class LocalRepositoryMetadataParserTest {
  @Test
  fun `parse sample plugins xml`() {
    val resourceUrl = LocalRepositoryMetadataParserTest::class.java.classLoader.getResource("plugins.xml")
    val xmlFile = File(resourceUrl.toURI())
    val parsed = LocalRepositoryMetadataParser().parseFromXml(xmlFile)
    val expected = listOf(
        LocalPluginInfo(
            "plugin.id",
            "1.0",
            "plugin name",
            IdeVersion.createIdeVersion("181.468"),
            IdeVersion.createIdeVersion("181.468"),
            "JetBrains",
            xmlFile.resolveSibling("file.zip")
        )
    )
    Assert.assertEquals(expected, parsed)
  }
}