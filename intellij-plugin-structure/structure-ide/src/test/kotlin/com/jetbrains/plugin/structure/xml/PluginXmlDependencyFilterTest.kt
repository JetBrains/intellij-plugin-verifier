/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.xml

import com.jetbrains.plugin.structure.ide.dependencies.PluginXmlDependencyFilter
import org.intellij.lang.annotations.Language
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

class PluginXmlDependencyFilterTest {

  @Test
  fun `XML for Java plugin with XML comment embedded in DTD entity is not parsed`() {
    // taken from plugins/grazie/lib/grazie.jar!org/languagetool/rules/en/grammar.xml
    @Language("XML") val pluginXml = """
      <?xml version="1.0"?>
      <!DOCTYPE rules [
              <!ENTITY rude_sarcastic_2 "(?:are you blind\?|are you deaf\?|bite me|I hope you're happy[\.!]|oh really\!?\?|since when\?|whoosh[\.!]|you don't say\?|you think\?)"><!-- XXX should be avoided in writing: what's new\?|what else is new| -->
      ]>
      <rules>
      </rules>
    """.trimIndent()

    val expectedXml = ""

    val pluginXmFilter = PluginXmlDependencyFilter()
    val filteredXml = captureToString {
      pluginXmFilter.filter(pluginXml.toInputStream(), this)
    }

    assertEquals(expectedXml, filteredXml)
  }

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

  @Test
  fun `non-plugin-XML with minimal structure and incorrect XML declaration is not even parsed`() {
    val xml = """
      <?xml?>
      <model-external-data>
      </model-external-data>      
    """.trimIndent()

    // STaX parser has issues with wrong PIs
    val expectedXml = ""

    val pluginXmFilter = PluginXmlDependencyFilter()
    val filteredXml: String = captureToString {
      pluginXmFilter.filter(xml.toInputStream(), this)
    }

    assertEquals(expectedXml, filteredXml)
  }

  @Test
  fun `non-plugin-XML with just PIs is parsed`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <?xml-stylesheet type="text/xsl" href="print.xsl" ?>
      <?xml-stylesheet type="text/css" href="rules.css" title="Easy editing stylesheet" ?>
      <rules>
      </rules>
    """.trimIndent()

    val expectedXml = """<?xml-stylesheet type="text/xsl" href="print.xsl" ?>""" +
    """<?xml-stylesheet type="text/css" href="rules.css" title="Easy editing stylesheet" ?>""" +
    "<rules>\n" +
    """</rules>""".trimIndent()

    val pluginXmFilter = PluginXmlDependencyFilter()
    val filteredXml: String = captureToString {
      pluginXmFilter.filter(xml.toInputStream(), this)
    }

    assertEquals(expectedXml, filteredXml)
  }

  @Test
  fun `plugin with declared modules in v1 and v2 versions is parsed`() {
    @Language("XML") val xml = """
      <idea-plugin>
        <id>com.intellij</id>
        <module value="com.intellij.modules.idea" />
        <content>
          <module name="intellij.platform.coverage"><![CDATA[<idea-plugin>
            <module value="com.intellij.modules.coverage" />
              <dependencies>
                <module name="com.intellij.modules.idea" />
              </dependencies>
            </module>                 
          </idea-plugin>
          ]]></module>
        </content>
      </idea-plugin>
    """.trimIndent()

    @Language("XML") val expectedXml = """
      <idea-plugin>
        <id>com.intellij</id>
        <module value="com.intellij.modules.idea"/>
        <content>
          <module name="intellij.platform.coverage"><![CDATA[<idea-plugin>
            <module value="com.intellij.modules.coverage" />
              <dependencies>
                <module name="com.intellij.modules.idea" />
              </dependencies>
            </module>                 
          </idea-plugin>
          ]]></module>
        </content>
      </idea-plugin>
    """.trimIndent()

    val pluginXmFilter = PluginXmlDependencyFilter()
    val filteredXml: String = captureToString {
      pluginXmFilter.filter(xml.toInputStream(), this)
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
    val resourceAsStream: InputStream? = PluginXmlDependencyFilterTest::class.java.getResourceAsStream(name)
    checkNotNull(resourceAsStream)
    return resourceAsStream
  }

  private fun String.toInputStream() = ByteArrayInputStream(this.toByteArray())
}