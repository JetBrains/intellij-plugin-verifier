package com.jetbrains.plugin.structure.ide.plugin

import org.junit.Assert.assertEquals
import org.junit.Test

class PluginIdExtractorTests {
  @Test
  fun `plugin XML ID is extracted`() {
    val pluginXml = """
        <idea-plugin xmlns:xi="http://www.w3.org/2001/XInclude">
          <name>Java</name>
          <id>com.intellij.java</id>
        </idea-plugin>
    """.trimIndent()

    val pluginIdExtractor = PluginIdExtractor()
    val pluginId = pluginIdExtractor.extractId(pluginXml.byteInputStream())

    assertEquals("com.intellij.java", pluginId)
  }

  @Test
  fun `plugin XML ID is extracted when there is inline CDATA with ID`() {
    val pluginXml = """
        <idea-plugin xmlns:xi="http://www.w3.org/2001/XInclude">
          <name>Java</name>
          <id>com.intellij.java</id>
          <content>
            <module name="intellij.ml.llm.privacy"><![CDATA[
              <idea-plugin package="com.intellij.ml.llm.privacy">
                <id>com.intellij.ml.llm.privacy</id>
              </idea-plugin>]]>
            </module>
          </content>                      
        </idea-plugin>
    """.trimIndent()

    val pluginIdExtractor = PluginIdExtractor()
    val pluginId = pluginIdExtractor.extractId(pluginXml.byteInputStream())

    assertEquals("com.intellij.java", pluginId)
  }
}