package com.jetbrains.pluginverifier.repository

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.checkHostIsAvailable
import com.jetbrains.pluginverifier.repository.repositories.custom.CustomPluginRepositoryProperties
import com.jetbrains.pluginverifier.repository.repositories.custom.MultiPushPluginRepository
import org.junit.Assert.*
import org.junit.Assume
import org.junit.Test
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

class MultiPushPluginRepositoryTest : BaseRepositoryTest<MultiPushPluginRepository>() {

  override fun createRepository(): MultiPushPluginRepository {
    val buildServerUrl = CustomPluginRepositoryProperties.MULTI_PUSH_PLUGIN_BUILD_SERVER_URL.getUrl()
    val sourceCodeUrl = CustomPluginRepositoryProperties.MULTI_PUSH_PLUGIN_SOURCE_CODE_URL.getUrl()

    Assume.assumeNotNull(buildServerUrl)
    Assume.assumeNotNull(sourceCodeUrl)

    Assume.assumeTrue(checkHostIsAvailable(buildServerUrl!!))
    Assume.assumeTrue(checkHostIsAvailable(sourceCodeUrl!!))

    return MultiPushPluginRepository(buildServerUrl, sourceCodeUrl)
  }

  @Test
  fun `request plugins list from real endpoint`() {
    val allPlugins = repository.getAllPlugins()
    assertFalse(allPlugins.isEmpty())
    val pluginInfo = allPlugins[0]
    assertEquals("vcs-hosting-multipush", pluginInfo.pluginId)
    assertEquals("Vcs Hosting Multi-Push", pluginInfo.pluginName)
    assertEquals("JetBrains", pluginInfo.vendor)
    assertNotNull(pluginInfo.sinceBuild)
    assertNotNull(pluginInfo.untilBuild)
    val buildServerUrl = CustomPluginRepositoryProperties.MULTI_PUSH_PLUGIN_BUILD_SERVER_URL.getUrl()
    assertEquals(buildServerUrl, pluginInfo.browserUrl)
    assertEquals(URL(buildServerUrl!!.toExternalForm().trimEnd('/') + "/.lastSuccessful/vcs-hosting-idea-multipush-${pluginInfo.version}.zip"), pluginInfo.downloadUrl)
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
    val document = parseDocument(
        """
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
    """.trimIndent()
    )

    val placeholderUrl = URL("https://example.com")
    val list = MultiPushPluginRepository.parsePluginsList(document, placeholderUrl, placeholderUrl)
    assertEquals(1, list.size)
    val pluginInfo = list[0]
    assertEquals(IdeVersion.createIdeVersion("182.671"), pluginInfo.sinceBuild)
    assertEquals(IdeVersion.createIdeVersion("183.1234"), pluginInfo.untilBuild)
    assertEquals("vcs-hosting-multipush", pluginInfo.pluginId)
    assertEquals("1.0.7", pluginInfo.version)
    assertEquals(URL(placeholderUrl.toExternalForm().trimEnd('/') + "/.lastSuccessful/vcs-hosting-idea-multipush-1.0.7.zip"), pluginInfo.downloadUrl)
    assertEquals(placeholderUrl, pluginInfo.browserUrl)
  }

}