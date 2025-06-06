/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.domain

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.intellij.plugin.PluginProvision
import com.jetbrains.plugin.structure.intellij.plugin.PluginProvision.Source.*
import com.jetbrains.plugin.structure.intellij.plugin.PluginQuery
import com.jetbrains.plugin.structure.mocks.modify
import com.jetbrains.plugin.structure.mocks.perfectXmlBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

private const val JAVA_PLUGIN_ID = "com.intellij.java"
private const val JAVA_PLUGIN_NAME = "Java"
private const val MODULE_ALIAS = "com.intellij.modules.java"

class IdePluginProviderTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var ide: Ide

  @Before
  fun setUp() {
    val ideRoot = buildDirectory(temporaryFolder.newFolder("idea").toPath()) {
      file("build.txt", "IE-221.5591.62")

      dir("lib") {
        zip("app.jar") {
          dir("META-INF") {
            file("PlatformLangPlugin.xml") {
              perfectXmlBuilder.modify {
                id = "<id>com.intellij</id>"
                name = "<name>IDEA CORE</name>"
                modules = listOf("some.idea.module")
              }
            }
          }
        }
      }

      dir("plugins") {
        dir("java") {
          dir("lib") {
            zip("java-impl.jar") {
              dir("META-INF") {
                file("plugin.xml") {
                  perfectXmlBuilder.modify {
                    id = "<id>$JAVA_PLUGIN_ID</id>"
                    name = "<name>$JAVA_PLUGIN_NAME</name>"
                    vendor = "<vendor>JetBrains</vendor>"
                    modules = listOf(MODULE_ALIAS)
                  }
                }
              }
            }
          }
        }
      }
    }
    ide = IdeManager.createManager().createIde(ideRoot)
  }

  @Test
  fun `resolve plugin by ID`() {
    val query =
      PluginQuery.Builder
        .of(JAVA_PLUGIN_ID)
        .inId()
        .build()
    val pluginProvision = ide.query(query)
    assertTrue(pluginProvision is PluginProvision.Found)
    pluginProvision as PluginProvision.Found
    with(pluginProvision) {
      assertEquals(JAVA_PLUGIN_ID, pluginProvision.plugin.pluginId)
      assertEquals(ID, pluginProvision.source)
    }
  }

  @Test
  fun `resolve plugin by name`() {
    val query = PluginQuery.Builder
      .of(JAVA_PLUGIN_NAME)
      .inName()
      .build()
    val pluginProvision = ide.query(query)
    assertTrue(pluginProvision is PluginProvision.Found)
    pluginProvision as PluginProvision.Found
    assertEquals(JAVA_PLUGIN_ID, pluginProvision.plugin.pluginId)
    assertEquals(JAVA_PLUGIN_NAME, pluginProvision.plugin.pluginName)
    assertEquals(NAME, pluginProvision.source)
  }

  @Test
  fun `resolve plugin by plugin alias`() {
    val query = PluginQuery.Builder
      .of(MODULE_ALIAS)
      .inPluginAliases()
      .build()
    val pluginProvision = ide.query(query)
    assertTrue(pluginProvision is PluginProvision.Found)
    pluginProvision as PluginProvision.Found
    assertEquals(JAVA_PLUGIN_ID, pluginProvision.plugin.pluginId)
    assertEquals(ALIAS, pluginProvision.source)
  }
}