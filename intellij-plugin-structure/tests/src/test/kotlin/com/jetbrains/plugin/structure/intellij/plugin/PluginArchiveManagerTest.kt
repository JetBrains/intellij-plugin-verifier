/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.utils.listFiles
import com.jetbrains.plugin.structure.base.utils.writeBytes
import com.jetbrains.plugin.structure.mocks.BaseFileSystemAwareTest
import com.jetbrains.plugin.structure.mocks.modify
import com.jetbrains.plugin.structure.mocks.perfectXmlBuilder
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream

class PluginArchiveManagerTest(fileSystemType: FileSystemType) : BaseFileSystemAwareTest(fileSystemType) {

  private lateinit var extractedPluginsPath: Path

  private lateinit var pluginArchiveManager: PluginArchiveManager

  @Before
  fun setUp() {
    extractedPluginsPath = temporaryFolder.newFolder("extracted-plugins")
    pluginArchiveManager = PluginArchiveManager(extractedPluginsPath)
  }

  @Test
  fun `archive is successfully extracted`() {
    val pluginArtifactPath = buildZipFile(temporaryFolder.newFile("plugin.zip")) {
      dir("plugin") {
        dir("lib") {
          zip("plugin.jar") {
            dir("META-INF") {
              file("plugin.xml") { perfectXmlBuilder.modify { } }
            }
          }
        }
      }
    }

    val archiveResult = pluginArchiveManager.extractArchive(pluginArtifactPath)
    assertTrue(archiveResult is PluginArchiveManager.Result.Extracted)
    archiveResult as PluginArchiveManager.Result.Extracted
    assertEquals(1, extractedPluginsPath.listFiles().size)
    assertTrue(extractedPluginsPath.contains(archiveResult))
  }

  @Test
  fun `malformed archive is not extracted`() {
    val pluginArtifactPath = temporaryFolder.newFile("plugin.zip")
    pluginArtifactPath.writeBytes(ByteArray(10) { it.toByte() })

    val archiveResult = pluginArchiveManager.extractArchive(pluginArtifactPath)
    assertTrue(archiveResult is PluginArchiveManager.Result.Failed)
    archiveResult as PluginArchiveManager.Result.Failed
    assertEquals(0, extractedPluginsPath.listFiles().size)
  }

  @Test
  fun `archive is successfully cached`() {
    val pluginArtifactPath = buildZipFile(temporaryFolder.newFile("plugin.zip")) {
      dir("plugin") {
        dir("lib") {
          zip("plugin.jar") {
            dir("META-INF") {
              file("plugin.xml") { perfectXmlBuilder.modify { } }
            }
          }
        }
      }
    }

    repeat(2) {
      val archiveResult = pluginArchiveManager.extractArchive(pluginArtifactPath)
      assertTrue(archiveResult is PluginArchiveManager.Result.Extracted)
      archiveResult as PluginArchiveManager.Result.Extracted
      assertEquals(1, extractedPluginsPath.listFiles().size)
      assertTrue(extractedPluginsPath.contains(archiveResult))
    }
  }

  @Test
  fun `archive is successfully retrieved, its resource is closed but it is retrieved again`() {
    val pluginArtifactPath = buildZipFile(temporaryFolder.newFile("plugin.zip")) {
      dir("plugin") {
        dir("lib") {
          zip("plugin.jar") {
            dir("META-INF") {
              file("plugin.xml") { perfectXmlBuilder.modify { } }
            }
          }
        }
      }
    }

    val firstArchiveResult = pluginArchiveManager.extractArchive(pluginArtifactPath)
    assertTrue(firstArchiveResult is PluginArchiveManager.Result.Extracted)
    firstArchiveResult as PluginArchiveManager.Result.Extracted
    assertEquals(1, extractedPluginsPath.listFiles().size)
    assertTrue(extractedPluginsPath.contains(firstArchiveResult))

    firstArchiveResult.resourceToClose.close()

    val secondArchive = pluginArchiveManager.extractArchive(pluginArtifactPath)
    assertTrue(secondArchive is PluginArchiveManager.Result.Extracted)
    secondArchive as PluginArchiveManager.Result.Extracted
    assertEquals(1, extractedPluginsPath.listFiles().size)
    assertTrue(extractedPluginsPath.contains(secondArchive))

    secondArchive.resourceToClose.close()
  }

  private fun Path.contains(result: PluginArchiveManager.Result.Extracted): Boolean {
    return Files.walk(this).use { stream: Stream<Path> ->
      stream.anyMatch { it == result.extractedPath }
    }
  }

  @After
  fun tearDown() {
    pluginArchiveManager.close()
  }
}