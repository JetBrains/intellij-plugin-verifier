/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.telemetry.PLUGIN_DESCRIPTOR_PARSER
import com.jetbrains.plugin.structure.base.utils.contentBuilder.ContentBuilder
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.intellij.resources.DefaultResourceResolver
import com.jetbrains.plugin.structure.jar.SingletonCachingJarFileSystemProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Which parser ran has to be observable from outside, or a corpus run cannot attribute a change in
 * verification results to the parser migration. This pins the reporting channel down across the plugin
 * layouts a real run actually meets - a JAR in `lib`, an exploded directory, and a plugin with content
 * modules - because the value is recorded per descriptor while only the top-level creator's telemetry is
 * reported, and those are easy to get out of step.
 */
class DescriptorParserTelemetryTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `a plugin the rule selects is reported as parsed by the platform parser`() {
    assertParser("platform", "selected-jar", untilBuild = "263.*") {
      dir("lib") {
        zip("main.jar") { dir("META-INF") { file("plugin.xml", pluginXml("263.*")) } }
      }
    }
  }

  @Test
  fun `a plugin the rule rejects is reported as parsed by the JAXB parser`() {
    assertParser("jaxb", "rejected-jar", untilBuild = "262.*") {
      dir("lib") {
        zip("main.jar") { dir("META-INF") { file("plugin.xml", pluginXml("262.*")) } }
      }
    }
  }

  @Test
  fun `the parser is reported for an exploded directory plugin`() {
    assertParser("platform", "selected-dir", untilBuild = "263.*") {
      dir("META-INF") { file("plugin.xml", pluginXml("263.*")) }
    }
  }

  @Test
  fun `the parser is reported once for a plugin whose content modules inherit the choice`() {
    // The modules are parsed by their own PluginCreators, whose telemetry is discarded; the single
    // reported value must still be the plugin's own.
    val plugin = build("with-content-modules") {
      dir("lib") {
        zip("main.jar") {
          dir("META-INF") {
            file("plugin.xml", pluginXml("263.*", content = """
              <content>
                <module name="example.file.based"/>
                <module name="example.inline"><![CDATA[<idea-plugin/>]]></module>
              </content>
            """.trimIndent()))
          }
        }
        zip("modules.jar") { file("example.file.based.xml", "<idea-plugin/>") }
      }
    }
    assertEquals(2, plugin.plugin.contentModules.size)
    assertEquals("platform", plugin.telemetry[PLUGIN_DESCRIPTOR_PARSER])
  }

  private fun assertParser(
    expected: String,
    name: String,
    untilBuild: String,
    content: ContentBuilder.() -> Unit
  ) {
    val result = build(name, content)
    assertEquals(
      "a plugin declaring until-build=$untilBuild must be reported as parsed by the $expected parser",
      expected,
      result.telemetry[PLUGIN_DESCRIPTOR_PARSER]
    )
  }

  private fun build(name: String, content: ContentBuilder.() -> Unit): PluginCreationSuccess<IdePlugin> {
    val pluginPath = buildDirectory(temporaryFolder.newFolder(name).toPath(), content)
    val manager = IdePluginManager.createManager(
      DefaultResourceResolver, temporaryFolder.newFolder().toPath(), SingletonCachingJarFileSystemProvider
    )
    val result = manager.createPlugin(pluginPath, false)
    assertTrue("expected a successfully created plugin but got $result", result is PluginCreationSuccess)
    return result as PluginCreationSuccess<IdePlugin>
  }

  private fun pluginXml(untilBuild: String, content: String = "") = """
    <idea-plugin>
      <id>com.example.telemetry</id>
      <name>Telemetry Example</name>
      <version>1.0</version>
      <vendor>Vendor Inc.</vendor>
      <description><![CDATA[A plugin used to check that the descriptor parser choice is reported.]]></description>
      <idea-version since-build="241.0" until-build="$untilBuild"/>
      <depends>com.intellij.modules.platform</depends>
      $content
    </idea-plugin>
  """.trimIndent()
}
