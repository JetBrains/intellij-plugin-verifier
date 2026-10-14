/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SharedContentModuleDependencyTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `plugin dependency shared by required and optional content modules is a single mandatory dependency`() {
    val plugin = buildPlugin()

    val sharedDependencies = plugin.dependencies.filter { it.id == SHARED_PLUGIN_ID }
    assertEquals("Expected a single '$SHARED_PLUGIN_ID' dependency but got $sharedDependencies", 1, sharedDependencies.size)
    assertFalse(sharedDependencies.single().isOptional)
  }

  private fun buildPlugin(): IdePlugin {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip").toPath()) {
      dir("plugin") {
        dir("lib") {
          zip("plugin.jar") {
            dir("META-INF") {
              file(
                "plugin.xml",
                """
                  <idea-plugin>
                    <id>com.example.plugin</id>
                    <name>Example plugin</name>
                    <version>1.0</version>
                    <vendor>JetBrains</vendor>
                    <description>Example plugin used to verify shared content module dependencies.</description>
                    <change-notes>Example plugin used to verify shared content module dependencies.</change-notes>
                    <idea-version since-build="241.0"/>
                    <content>
                      <module name="com.example.plugin.required" loading="required"/>
                      <module name="com.example.plugin.optional" loading="optional"/>
                    </content>
                  </idea-plugin>
                """.trimIndent()
              )
            }
          }
          zip("required.jar") {
            file("com.example.plugin.required.xml", moduleDescriptor)
          }
          zip("optional.jar") {
            file("com.example.plugin.optional.xml", moduleDescriptor)
          }
        }
      }
    }
    val creationResult = IdePluginManager.createManager().createPlugin(pluginFile, validateDescriptor = true)
    if (creationResult !is PluginCreationSuccess) {
      fail("Expected successful plugin creation, but got $creationResult")
    }
    return (creationResult as PluginCreationSuccess).plugin
  }

  private val moduleDescriptor = """
    <idea-plugin>
      <dependencies>
        <plugin id="$SHARED_PLUGIN_ID"/>
      </dependencies>
    </idea-plugin>
  """.trimIndent()
}

private const val SHARED_PLUGIN_ID = "com.example.shared"
