package com.jetbrains.pluginverifier.repository

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.repositories.custom.MultiPushPluginRepository
import com.jetbrains.pluginverifier.results.HostReachableRule
import org.junit.Assert.*
import org.junit.Test
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

@HostReachableRule.HostReachable("https://google.com")
class MultiPushPluginRepositoryTest : BaseRepositoryTest<MultiPushPluginRepository>() {

  companion object {
    val buildServerUrl = URL("https://buildserver.labs.intellij.net")
  }

  override fun createRepository() = MultiPushPluginRepository(buildServerUrl)

  @Test
  fun `plugin list`() {
    val allPlugins = repository.getAllPlugins()
    assertFalse(allPlugins.isEmpty())
    val pluginInfo = allPlugins[0]
    assertEquals("vcs-hosting-multipush", pluginInfo.pluginId)
    assertEquals("Vcs Hosting Multi-Push", pluginInfo.pluginName)
    assertEquals("JetBrains", pluginInfo.vendor)
    assertNotNull(pluginInfo.sinceBuild)
    assertNotNull(pluginInfo.untilBuild)
    assertEquals(URL(buildServerUrl, "guestAuth/repository/download/ijplatform_master_Idea_Experiments_BuildMultiPushPlugin"), pluginInfo.browserUrl)
    assertEquals(URL(buildServerUrl, "guestAuth/repository/download/ijplatform_master_Idea_Experiments_BuildMultiPushPlugin/.lastSuccessful/vcs-hosting-idea-multipush-${pluginInfo.version}.zip"), pluginInfo.downloadUrl)
  }

  @Test
  fun `download plugin`() {
    val allPlugins = repository.getAllPlugins()
    val pluginInfo = allPlugins[0]
    checkDownloadPlugin(pluginInfo)
  }

  private fun parseDocument(pluginsXml: String): Document {
    return DocumentBuilderFactory.newInstance()
        .newDocumentBuilder()
        .parse(InputSource(ByteArrayInputStream(pluginsXml.toByteArray())))
  }

  @Test
  fun `parse plugins list for Multi-Push plugin`() {
    val document = parseDocument("""
      <plugin-repository>
<category name="VCS Integration">
<idea-plugin size="286692">
<name>Vcs Hosting Multi-Push</name>
<id>vcs-hosting-multipush</id>
<version>1.0.7</version>
<idea-version since-build="182.671" until-build="183.1234"/>
<vendor>JetBrains</vendor>
<download-url>vcs-hosting-idea-multipush-1.0.7.zip</download-url>
<description>
<![CDATA[
Supports the multipush feature of the git-hosting, which allows to push commits from different repositories synchronously, using the repo-multi-push command implemented on the server.
]]>
</description>
</idea-plugin>
</category>
</plugin-repository>
    """.trimIndent())

    val list = MultiPushPluginRepository.parsePluginsList(document, buildServerUrl)
    assertEquals(1, list.size)
    val pluginInfo = list[0]
    assertEquals(IdeVersion.createIdeVersion("182.671"), pluginInfo.sinceBuild)
    assertEquals(IdeVersion.createIdeVersion("183.1234"), pluginInfo.untilBuild)
    assertEquals("vcs-hosting-multipush", pluginInfo.pluginId)
    assertEquals("1.0.7", pluginInfo.version)
    assertEquals(URL(buildServerUrl, "guestAuth/repository/download/ijplatform_master_Idea_Experiments_BuildMultiPushPlugin/.lastSuccessful/vcs-hosting-idea-multipush-1.0.7.zip"), pluginInfo.downloadUrl)
    assertEquals(URL(buildServerUrl, "guestAuth/repository/download/ijplatform_master_Idea_Experiments_BuildMultiPushPlugin"), pluginInfo.browserUrl)
  }

}