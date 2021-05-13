package com.jetbrains.plugin.structure.repository

import com.jetbrains.plugin.structure.intellij.repository.CustomPluginRepositoryListingParser
import com.jetbrains.plugin.structure.intellij.repository.CustomPluginRepositoryListingType
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.junit.Assert
import org.junit.Test
import java.net.URL

class CustomPluginRepositoryListingParserTest {
  @Test
  fun `parse simple format`() {
    val xmlContent = """
      <plugins>
        <plugin id="pluginId" url="file.zip" version="1.0"/>
        
        <plugin id="pluginId2" url="https://absolute-url.com/file.zip" version="1.0" name="pluginName2" vendor="pluginVendor2">
          <idea-version since-build="193.0" until-build="201.1"/>
        </plugin>
      </plugins>
      """.trimIndent()

    val xmlUrl = URL("https://xml-url.com")
    val repositoryUrl = URL("https://browser-url.com")

    val expectedInfos = listOf(
      CustomPluginRepositoryListingParser.PluginInfo(
        "pluginId",
        "pluginId",
        "1.0",
        null,
        repositoryUrl,
        URL("https://xml-url.com/file.zip"),
        repositoryUrl,
        null,
        null,
        null
      ),
      CustomPluginRepositoryListingParser.PluginInfo(
        "pluginId2",
        "pluginName2",
        "1.0",
        "pluginVendor2",
        repositoryUrl,
        URL("https://absolute-url.com/file.zip"),
        repositoryUrl,
        null,
        IdeVersion.createIdeVersion("193.0"),
        IdeVersion.createIdeVersion("201.1")
      )
    )

    val actualInfos = CustomPluginRepositoryListingParser.parseListOfPlugins(
      xmlContent,
      xmlUrl,
      repositoryUrl,
      CustomPluginRepositoryListingType.SIMPLE
    )

    Assert.assertEquals(expectedInfos.size, actualInfos.size)
    expectedInfos.forEachIndexed { index, expectedInfo ->
      assertCustomPluginInfosAreTheSame(expectedInfo, actualInfos[index])
    }
  }

  @Test
  fun `parse plugin repository format`() {
    val xmlContent = """
      <plugin-repository>
       <category name="CategoryName">
         <idea-plugin size="42">
           <id>com.some.id</id>
           <name>Plugin Name</name>
           <version>1.0</version>
           <idea-version since-build="193.0" until-build="201.1"/>
           <vendor>VendorName</vendor>
           <download-url>file.zip</download-url>
           <description>
             <![CDATA[
             Some description here.
             ]]>
           </description>
         </idea-plugin>
       </category>
      </plugin-repository>
    """.trimIndent()

    val xmlUrl = URL("https://xml-url.com")
    val repositoryUrl = URL("https://browser-url.com")

    val expectedInfos = listOf(
      CustomPluginRepositoryListingParser.PluginInfo(
        "com.some.id",
        "Plugin Name",
        "1.0",
        "VendorName",
        repositoryUrl,
        URL("https://xml-url.com/file.zip"),
        repositoryUrl,
        null,
        IdeVersion.createIdeVersion("193.0"),
        IdeVersion.createIdeVersion("201.1")
      )
    )

    val actualInfos = CustomPluginRepositoryListingParser.parseListOfPlugins(
      xmlContent,
      xmlUrl,
      repositoryUrl,
      CustomPluginRepositoryListingType.PLUGIN_REPOSITORY
    )

    assertCustomPluginInfoListsAreTheSame(expectedInfos, actualInfos)
  }

  companion object {
    fun assertCustomPluginInfosAreTheSame(expectedInfo: CustomPluginRepositoryListingParser.PluginInfo, actualInfo: CustomPluginRepositoryListingParser.PluginInfo, checkVersions: Boolean = true) {
      Assert.assertEquals(expectedInfo.pluginId, actualInfo.pluginId)
      Assert.assertEquals(expectedInfo.pluginName, actualInfo.pluginName)
      if (checkVersions) {
        Assert.assertEquals(expectedInfo.version, actualInfo.version)
      }
      Assert.assertEquals(expectedInfo.vendor, actualInfo.vendor)
      Assert.assertEquals(expectedInfo.browserUrl, actualInfo.browserUrl)
      Assert.assertEquals(expectedInfo.downloadUrl, actualInfo.downloadUrl)
      Assert.assertEquals(expectedInfo.sourceCodeUrl, actualInfo.sourceCodeUrl)
      Assert.assertEquals(expectedInfo.sinceBuild, actualInfo.sinceBuild)
      Assert.assertEquals(expectedInfo.untilBuild, actualInfo.untilBuild)
    }

    fun assertCustomPluginInfoListsAreTheSame(expectedInfos: List<CustomPluginRepositoryListingParser.PluginInfo>, actualInfos: List<CustomPluginRepositoryListingParser.PluginInfo>, checkVersions: Boolean = true) {
      Assert.assertEquals(expectedInfos.size, actualInfos.size)
      expectedInfos.forEachIndexed { index, expectedInfo ->
        assertCustomPluginInfosAreTheSame(expectedInfo, actualInfos[index], checkVersions)
      }
    }
  }
}