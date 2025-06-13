/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.pluginverifier.plugin.DefaultPluginDetailsProvider
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.plugin.resolution.PluginInfo
import com.jetbrains.pluginverifier.repository.files.IdleFileLock
import com.jetbrains.pluginverifier.repository.repositories.dependency.DependencyPluginInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Path

class PluginDetailsProviderTest : BasePluginTest() {

  private lateinit var extractedPath: Path

  @Before
  fun setUp() {
    extractedPath = temporaryFolder.newFolder("extracted").toPath()
  }

  @Test
  fun `plugin info is cached for ZIPed plugins`() {
    val header = ideaPlugin("com.example")
    val pluginZipPath = buildZipFile(temporaryFolder.newFile("plugin.zip").toPath()) {
      dir("SomePlugin") {
        dir("lib") {
          zip("plugin.jar") {
            dir("META-INF") {
              file("plugin.xml") {
                """
                  <idea-plugin>
                    $header
                  </idea-plugin>
                """
              }
            }
          }
        }
      }
    }

    val pluginInfo = DependencyPluginInfo(PluginInfo("com.example", "SomePlugin", "1"))

    DefaultPluginDetailsProvider(extractedPath).use { provider ->
      repeat(2) {
        val pluginDetailsResult = provider.providePluginDetails(pluginInfo, IdleFileLock(pluginZipPath))
        assertTrue(pluginDetailsResult is PluginDetailsProvider.Result.Provided)
      }
      // the second access is cached
      assertEquals(1, provider.closeableResourcesSize)
      with(provider.eventLog) {
        assertEquals(2, size)
        assertEquals("extracted $pluginZipPath", this[0])
        assertEquals("cached $pluginZipPath", this[1])
      }
    }
  }
}