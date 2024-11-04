package com.jetbrains.plugin.structure.xml

import com.jetbrains.plugin.structure.ide.dependencies.PluginXmlDependencyFilter
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

class XmlTest {
  @Test
  fun `XML for Java plugin is filtered`() {
    val pluginXmFilter = PluginXmlDependencyFilter()
    resourceStream("/plugin-com-intellij-java-nocdata.xml").use { inXmlStream ->
      resourceStream("/plugin-com-intellij-java-nocdata-expected.xml")
        .use { expectedXmlStream ->
          val expectedXml = expectedXmlStream.bufferedReader().use(BufferedReader::readText)

          val filteredXml = captureToString {
            pluginXmFilter.filter(inXmlStream, this)
          }
          assertEquals(expectedXml, filteredXml)
        }
    }
  }

  @Test
  fun `XML for just-dependencies plugin is filtered`() {
    val pluginXml = """
      <idea-plugin>
        <dependencies>
          <module id="some.module" />
          <plugin id="some.plugin" />
        </dependencies>
      </idea-plugin>
    """.trimIndent()

    val expectedXml = """
      <idea-plugin>
        <dependencies>
          <module id="some.module"/>
          <plugin id="some.plugin"/>
        </dependencies>
      </idea-plugin>
    """.trimIndent()

    val pluginXmFilter = PluginXmlDependencyFilter()
    val filteredXml = captureToString {
      pluginXmFilter.filter(pluginXml.toInputStream(), this)
    }

    assertEquals(expectedXml, filteredXml)
  }

  @Test
  fun `XML for simple plugin is filtered`() {
    val pluginXml = """
      <idea-plugin>
        <id>com.intellij.java</id>
        <category>Languages</category>
        <dependencies>
          <module id="some.module" />
          <plugin id="some.plugin" />
        </dependencies>
      </idea-plugin>
    """.trimIndent()

    val expectedXml = """
      <idea-plugin>
        <id>com.intellij.java</id>
        <dependencies>
          <module id="some.module"/>
          <plugin id="some.plugin"/>
        </dependencies>
      </idea-plugin>
    """.trimIndent()

    val pluginXmFilter = PluginXmlDependencyFilter()
    val filteredXml = captureToString {
      pluginXmFilter.filter(pluginXml.toInputStream(), this)
    }

    assertEquals(expectedXml, filteredXml)
  }

  @Test
  fun `XML for plugin with declared modules is filtered`() {
    val pluginXml = """
      <idea-plugin>
        <id>com.intellij.java</id>
        <module value="com.intellij.modules.someModule" />        
        <dependencies>
          <module id="some.module" />
        </dependencies>
      </idea-plugin>
    """.trimIndent()

    val expectedXml = """
      <idea-plugin>
        <id>com.intellij.java</id>
        <module value="com.intellij.modules.someModule"/>        
        <dependencies>
          <module id="some.module"/>
        </dependencies>
      </idea-plugin>
    """.trimIndent()

    val pluginXmFilter = PluginXmlDependencyFilter()
    val filteredXml: String = captureToString {
      pluginXmFilter.filter(pluginXml.toInputStream(), this)
    }

    assertEquals(expectedXml, filteredXml)
  }

  private fun captureToString(capturer: ByteArrayOutputStream.() -> Unit): String {
    return ByteArrayOutputStream().use {
      capturer(it)
      it.toString(Charsets.UTF_8)
    }
  }

  private fun resourceStream(name: String): InputStream {
    val resourceAsStream: InputStream? = XmlTest::class.java.getResourceAsStream(name)
    checkNotNull(resourceAsStream)
    return resourceAsStream
  }

  private fun String.toInputStream() = ByteArrayInputStream(this.toByteArray())
}