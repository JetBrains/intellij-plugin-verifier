/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.base.utils.listFiles
import com.jetbrains.plugin.structure.intellij.problems.AnyProblemToWarningPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.resources.PluginArchiveResource
import com.jetbrains.plugin.structure.mocks.IdePluginManagerTest
import com.jetbrains.plugin.structure.mocks.modify
import com.jetbrains.plugin.structure.mocks.perfectXmlBuilder
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Path

class PluginExtractionTest(fileSystemType: FileSystemType) : IdePluginManagerTest(fileSystemType) {
  @Test
  fun `plugin is extracted, successfully constructed and the extraction directory remains undeleted`() {
    val pluginFactory = { pluginManager: IdePluginManager, pluginArtifactPath: Path ->
      pluginManager.createPlugin(
        pluginArtifactPath,
        validateDescriptor = true,
        problemResolver = AnyProblemToWarningPluginCreationResultResolver,
      )
    }

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
    val successResult = createPluginSuccessfully(pluginArtifactPath, pluginFactory)
    assertEquals(1, successResult.resources.size)
    val resource = successResult.resources.first()
    assertTrue(resource is PluginArchiveResource)
    resource as PluginArchiveResource
    assertTrue(resource.extractedPath.isDirectory)
    resource.delete()
    assertFalse(resource.extractedPath.isDirectory)
  }

  @Test
  fun `plugin is extracted, intentionally failed on construction, but the extraction directory remains undeleted`() {
    val failingProblemRemapper = object : PluginCreationResultResolver {
      override fun resolve(plugin: IdePlugin, problems: List<PluginProblem> ): PluginCreationResult<IdePlugin> {
        return PluginCreationFail(problems)
      }
    }

    val pluginFactory = { pluginManager: IdePluginManager, pluginArtifactPath: Path ->
      pluginManager.createPlugin(
        pluginArtifactPath,
        validateDescriptor = true,
        problemResolver = failingProblemRemapper,
      )
    }

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
    assertProblematicPlugin(pluginArtifactPath, emptyList(), pluginFactory)
    assertEquals(1, extractedDirectory.listFiles().size)
  }

  @After
  fun tearDown() {
    close()
  }
}