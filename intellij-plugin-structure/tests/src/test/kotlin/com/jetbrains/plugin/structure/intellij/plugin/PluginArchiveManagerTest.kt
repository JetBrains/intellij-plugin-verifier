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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream

class PluginArchiveManagerTest(fileSystemType: FileSystemType) : BaseFileSystemAwareTest(fileSystemType) {
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

    val extractedPluginsPath = temporaryFolder.newFolder("extracted-plugins")
    val pluginArchiveManager = PluginArchiveManager(extractedPluginsPath)
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

    val extractedPluginsPath = temporaryFolder.newFolder("extracted-plugins")
    val pluginArchiveManager = PluginArchiveManager(extractedPluginsPath)
    val archiveResult = pluginArchiveManager.extractArchive(pluginArtifactPath)
    assertTrue(archiveResult is PluginArchiveManager.Result.Failed)
    archiveResult as PluginArchiveManager.Result.Failed
    assertEquals(0, extractedPluginsPath.listFiles().size)
  }

  private fun Path.contains(result: PluginArchiveManager.Result.Extracted): Boolean {
    return Files.walk(this).use { stream: Stream<Path> ->
      stream.anyMatch { it == result.extractedPath }
    }
  }
}